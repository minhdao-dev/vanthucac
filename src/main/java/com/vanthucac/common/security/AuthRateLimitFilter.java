package com.vanthucac.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final long WINDOW_MILLIS = 60_000L;

    private static final Map<String, Integer> RATE_LIMITS = Map.of(
            "/api/v1/auth/login", 10,
            "/api/v1/auth/register", 5,
            "/api/v1/auth/refresh", 20
    );

    private final ConcurrentHashMap<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        var path = request.getRequestURI();
        var maxRequests = RATE_LIMITS.get(path);

        if (maxRequests == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var ip = extractClientIp(request);
        var key = ip + ":" + path;
        var now = System.currentTimeMillis();

        var timestamps = requestHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                log.warn("Rate limit exceeded — path: {}", path);
                sendTooManyRequestsResponse(response, path);
                return;
            }

            timestamps.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response, String path)
            throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = Map.of(
                "status", 429,
                "title", "Too Many Requests",
                "detail", "Rate limit exceeded for " + path + ". Please wait and try again.",
                "errorCode", "RATE_LIMIT_EXCEEDED"
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}