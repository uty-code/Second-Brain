package com.aimsgraph.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.redisson.api.RRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.web.context.request.RequestAttributes;

class JwtInterceptorTest {

    private final RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
    private final RRateLimiter rateLimiter = Mockito.mock(RRateLimiter.class);
    private final JwtInterceptor interceptor;
    
    public JwtInterceptorTest() {
        Mockito.when(redissonClient.getRateLimiter(Mockito.anyString())).thenReturn(rateLimiter);
        Mockito.when(rateLimiter.tryAcquire(1)).thenReturn(true);
        interceptor = new JwtInterceptor(redissonClient);
    }

    @Test
    void preHandle_AllowsWhenRateLimitAcquired() throws Exception {
        // Arrange
        String workspaceId = "test-vault";

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestContextHolder.getRequestAttributes().setAttribute(
                JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, workspaceId, RequestAttributes.SCOPE_REQUEST);

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void preHandle_RateLimitExceeded_Returns429() throws Exception {
        // Arrange
        String workspaceId = "test-vault";
        Mockito.when(rateLimiter.tryAcquire(1)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestContextHolder.getRequestAttributes().setAttribute(
                JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, workspaceId, RequestAttributes.SCOPE_REQUEST);

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }
}
