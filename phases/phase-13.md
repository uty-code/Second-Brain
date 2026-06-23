Status: [완료됨]

# Phase 13: B2B 로그아웃 기능 구현

## 목표
- 프론트엔드: 글로벌 네비게이션 로그아웃 버튼 (lucide-react LogOut 아이콘)
- 클릭 시 상태(useAppStore) 초기화 및 `/login` 리다이렉트
- 백엔드: `POST /v1/auth/logout` API
- Redis `RedissonClient` 블랙리스트(Blacklisting) 구현
- `JwtInterceptor`에서 블랙리스트 검증 처리 (401 Unauthorized)

## 진행 히스토리
- 2026-06-11: `AuthController`에 logout 엔드포인트 추가 및 `RedissonClient` 연동
- 2026-06-11: `JwtInterceptor`에 블랙리스트 확인 로직 작성
- 2026-06-11: `JwtUtil` 만료 시간(TTL) 계산 메서드(`getExpirationFromToken`) 추가
- 2026-06-11: 프론트엔드 `api.ts`에 `logoutUser` 호출 연동
- 2026-06-11: 프론트엔드 `Sidebar.tsx`에 `LogOut` 아이콘 추가 및 Zustand 상태 초기화 로직 구현
- 2026-06-11: 백엔드 `src/test/`에 TDD 기반 `AuthControllerTest`, `JwtInterceptorBlacklistTest` 테스트 통과 완료.
