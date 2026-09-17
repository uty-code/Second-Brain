---
title: "Phase 12: "
phase_number: 12
status: "completed"
created_at: 2026-06-02
updated_at: 2026-07-18
---

# Phase 12: B2B ?ㅼ쨷 ?ъ슜??濡쒓렇??諛??몄쬆 ?뚯씠?꾨씪??援ъ텞

- **?덉뒪?좊━**:
  - `aims-backend`: Spring Security (`spring-boot-starter-security`), `jjwt` 愿???섏〈?깆쓣 `build.gradle`??異붽?.
  - `aims-backend`: `Users` ?뚯씠釉?異붽? (`schema.sql`), Entity 諛?MyBatis Mapper 援ы쁽.
  - `aims-backend`: `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthController` 異붽??섏뿬 媛??濡쒓렇???뚯씠?꾨씪??援ы쁽 (鍮꾨??? `aimsgraph-jwt-secret-key-1234567890`, `BCryptPasswordEncoder` ?곸슜).
  - `aims-backend`: 湲곗〈 `JwtInterceptor`??429 Rate Limiting ?꾨떞?쇰줈 ??븷 異뺤냼 諛?ContextHolder ?곕룞.
  - `frontend`: `useAppStore`??`jwtToken`, `isLoggedIn`, `currentUser` ?몄쬆 ?곹깭 異붽? (zustand persist ?곸슜).
  - `frontend`: `api.ts`??紐⑤뱺 `fetch`??`Authorization: Bearer ${jwtToken}` ?숈쟻 ?ㅻ뜑 ?곸슜 援ы쁽.
  - `frontend`: 紐⑤뜕?섍퀬 ?몃젴??`login/page.tsx` ?붿옄??諛??쇱슦??媛쒕컻 (Next.js App Router).
  - `frontend`: `components/AuthGuard.tsx` ?묒꽦 ??`layout.tsx`???곸슜???쇱슦??媛??異붽?.
