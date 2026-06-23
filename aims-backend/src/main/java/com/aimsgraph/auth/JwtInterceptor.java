package com.aimsgraph.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RedissonClient;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String WORKSPACE_ID_ATTRIBUTE = "workspaceId";
    
    private final RedissonClient redissonClient;

    public JwtInterceptor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (redissonClient.getBucket("blacklist:" + token).isExists()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: Token is blacklisted");
                return false;
            }
        }

        String workspaceId = null;
        if (RequestContextHolder.getRequestAttributes() != null) {
            workspaceId = (String) RequestContextHolder.getRequestAttributes().getAttribute(
                    WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        }

        if (workspaceId != null && !workspaceId.isEmpty()) {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter("rate_limiter:" + workspaceId);
            // 50 requests per minute
            rateLimiter.trySetRate(RateType.OVERALL, 50, 1, RateIntervalUnit.MINUTES);
            
            if (!rateLimiter.tryAcquire(1)) {
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("Too Many Requests");
                return false;
            }
        }

        return true;
    }
}
