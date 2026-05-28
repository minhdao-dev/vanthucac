package com.vanthucac.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Order(2)
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitFilter.class);

    private static final String INCR_SCRIPT = """
            local key = KEYS[1]
            local ttl = ARGV[1]
            local count = redis.call('INCR', key)
            if count == 1 then
                redis.call('EXPIRE', key, ttl)
            end
            return count
            """;

    private static final RedisScript<Long> INCR_WITH_TTL = RedisScript.of(INCR_SCRIPT, Long.class);

    private static final List<RateLimitRule> RULES = List.of(
            new RateLimitRule(HttpMethod.POST, "/api/v1/auth/login", 10, Duration.ofSeconds(60)),
            new RateLimitRule(HttpMethod.POST, "/api/v1/auth/register", 5, Duration.ofSeconds(60)),
            new RateLimitRule(HttpMethod.POST, "/api/v1/auth/refresh", 20, Duration.ofSeconds(60)),
            new RateLimitRule(HttpMethod.POST, "/api/v1/auction-items/*/bids", 10, Duration.ofSeconds(10)),
            new RateLimitRule(HttpMethod.POST, "/api/v1/uploads/presigned-url", 20, Duration.ofHours(1))
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var path = request.getRequestURI();
        var rule = findMatchingRule(request.getMethod(), path);

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var ip = extractClientIp(request);
            var key = buildRedisKey(ip, rule, path);
            var currentCount = incrementAtomic(key, rule.window());

            if (currentCount > rule.limit()) {
                log.warn("Rate limit exceeded — method: {}, path: {}, ip: {}",
                        request.getMethod(), path, ip);
                sendTooManyRequestsResponse(response, rule, path);
                return;
            }
        } catch (RedisSystemException ex) {
            log.error("Redis rate limiter failed, allowing request — path: {}", path, ex);
        }

        filterChain.doFilter(request, response);
    }

    private long incrementAtomic(String key, Duration ttl) {
        var result = redisTemplate.execute(
                INCR_WITH_TTL,
                List.of(key),
                String.valueOf(ttl.toSeconds())
        );
        return Objects.requireNonNull(result, "Lua INCR_WITH_TTL must return a value");
    }

    private RateLimitRule findMatchingRule(String method, String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .findFirst()
                .orElse(null);
    }

    private String buildRedisKey(String ip, RateLimitRule rule, String path) {
        return "rate_limit:" + ip + ":" + rule.keySuffix(path);
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = getHeaderValue(request, "X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }

        var realIp = getHeaderValue(request, "X-Real-IP");
        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String getHeaderValue(HttpServletRequest request, String headerName) {
        var value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void sendTooManyRequestsResponse(
            HttpServletResponse response,
            RateLimitRule rule,
            String path
    ) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = Map.of(
                "success", false,
                "message", "Rate limit exceeded. Please wait and try again.",
                "data", Map.of(
                        "errorCode", "RATE_LIMIT_EXCEEDED",
                        "path", path,
                        "limit", rule.limit(),
                        "windowSeconds", rule.window().toSeconds()
                )
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}