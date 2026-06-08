package com.aimsgraph.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.redisson.api.RRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;

class JwtInterceptorTest {

    private final String secret = "test-secret-key-that-is-very-long-and-secure";
    private final RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
    private final RRateLimiter rateLimiter = Mockito.mock(RRateLimiter.class);
    private final JwtInterceptor interceptor;
    
    public JwtInterceptorTest() {
        Mockito.when(redissonClient.getRateLimiter(Mockito.anyString())).thenReturn(rateLimiter);
        Mockito.when(rateLimiter.tryAcquire(1)).thenReturn(true);
        interceptor = new JwtInterceptor(secret, redissonClient);
    }

    @Test
    void generateAndPrintToken() {
        String realSecret = "your-super-secret-key-for-aims-graph-backend-dev-only";
        String token = Jwts.builder()
                .subject("test-workspace")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(realSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
        System.out.println("\n\n=== [TEST JWT TOKEN] ===");
        System.out.println("Bearer " + token);
        System.out.println("========================\n\n");
    }

    @Test
    void preHandle_ValidToken_SetsWorkspaceId() throws Exception {
        // Arrange
        String workspaceId = "test-vault";
        String token = Jwts.builder()
                .subject(workspaceId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isTrue();
        assertThat(RequestContextHolder.currentRequestAttributes().getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, 0))
                .isEqualTo(workspaceId);
    }

    @Test
    void preHandle_InvalidToken_Returns401() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
