package com.aimsgraph.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.mockito.Mockito;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;

class JwtInterceptorBlacklistTest {

    @Test
    void preHandle_BlacklistedToken_Returns401() throws Exception {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RBucket<Object> bucket = Mockito.mock(RBucket.class);
        
        Mockito.when(redissonClient.getBucket("blacklist:blacklisted_token")).thenReturn(bucket);
        Mockito.when(bucket.isExists()).thenReturn(true);
        
        JwtInterceptor interceptor = new JwtInterceptor(redissonClient);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer blacklisted_token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
