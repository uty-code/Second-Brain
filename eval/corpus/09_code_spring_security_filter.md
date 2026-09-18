# Spring Security 커스텀 JWT 인증 필터 구현

다음은 Spring Security 필터 체인에 삽입하여 HTTP Authorization 헤더의 Bearer 토큰을 검증하는 커스텀 필터 예시입니다.

```java
package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (tokenProvider.validateToken(token)) {
        String username = tokenProvider.getUsername(token);
        var auth = new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    filterChain.doFilter(request, response);
  }
}
```

`OncePerRequestFilter`를 상속하여 요청당 1회만 필터 로직이 실행되도록 보장합니다.
인증에 성공하면 `SecurityContextHolder`에 인증 토큰을 적재하여 하위 컨트롤러에서 유저 정보를 조회할 수 있게 합니다.
