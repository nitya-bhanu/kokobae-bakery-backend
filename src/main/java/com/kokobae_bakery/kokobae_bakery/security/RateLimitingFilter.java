package com.kokobae_bakery.kokobae_bakery.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, RateLimitEntry> ipRequestCounts = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowDurationMillis;

    public RateLimitingFilter(
            @Value("${rate-limiting.auth.max-requests:5}") int maxRequests,
            @Value("${rate-limiting.auth.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowDurationMillis = windowSeconds * 1000;
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(ipRequestCounts::clear, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RateLimitEntry entry = ipRequestCounts.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart > windowDurationMillis) {
                return new RateLimitEntry(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (entry.count > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many login/auth attempts. Please wait 1 minute before trying again.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}