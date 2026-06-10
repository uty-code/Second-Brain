> **Status: [완료됨]**

# Phase 12: B2B 다중 사용자 로그인 및 인증 파이프라인 구축

- **히스토리**:
  - `aims-backend`: Spring Security (`spring-boot-starter-security`), `jjwt` 관련 의존성을 `build.gradle`에 추가.
  - `aims-backend`: `Users` 테이블 추가 (`schema.sql`), Entity 및 MyBatis Mapper 구현.
  - `aims-backend`: `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthController` 추가하여 가입/로그인 파이프라인 구현 (비밀키: `aimsgraph-jwt-secret-key-1234567890`, `BCryptPasswordEncoder` 적용).
  - `aims-backend`: 기존 `JwtInterceptor`는 429 Rate Limiting 전담으로 역할 축소 및 ContextHolder 연동.
  - `frontend`: `useAppStore`에 `jwtToken`, `isLoggedIn`, `currentUser` 인증 상태 추가 (zustand persist 적용).
  - `frontend`: `api.ts`의 모든 `fetch`에 `Authorization: Bearer ${jwtToken}` 동적 헤더 적용 구현.
  - `frontend`: 모던하고 세련된 `login/page.tsx` 디자인 및 라우트 개발 (Next.js App Router).
  - `frontend`: `components/AuthGuard.tsx` 작성 후 `layout.tsx`에 적용해 라우트 가드 추가.
