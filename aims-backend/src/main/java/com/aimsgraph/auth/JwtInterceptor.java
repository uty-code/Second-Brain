package com.aimsgraph.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RedissonClient;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String WORKSPACE_ID_ATTRIBUTE = "workspaceId";
    
    private final SecretKey key;
    private final RedissonClient redissonClient;

    public JwtInterceptor(@Value("${jwt.secret:your-super-secret-key-for-aims-graph-backend-dev-only}") String secret, RedissonClient redissonClient) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
            
            try {
                String workspaceId = request.getHeader("X-Workspace-ID");
                if (workspaceId == null || workspaceId.isEmpty()) {
                    if ("MVP_DUMMY_TOKEN".equals(token)) {
                        workspaceId = "default-workspace";
                    } else {
                        Claims claims = Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        workspaceId = claims.getSubject();
                    }
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
                    
                    RequestContextHolder.currentRequestAttributes().setAttribute(
                            WORKSPACE_ID_ATTRIBUTE, workspaceId, RequestAttributes.SCOPE_REQUEST);
                    return true;
                }
            } catch (Exception e) {
                // 토큰 만료 또는 서명 검증 실패
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
