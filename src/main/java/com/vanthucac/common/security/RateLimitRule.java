package com.vanthucac.common.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;

public record RateLimitRule(
        HttpMethod method,
        String pathPattern,
        long limit,
        Duration window
) {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public boolean matches(String requestMethod, String requestPath) {
        return method.matches(requestMethod) && PATH_MATCHER.match(pathPattern, requestPath);
    }

    public String keySuffix(String requestPath) {
        return method.name() + ":" + pathPattern + ":" + requestPath;
    }
}