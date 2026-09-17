---
title: "Phase 13: "
phase_number: 13
status: "completed"
created_at: 2026-06-02
updated_at: 2026-07-18
---

# Phase 13: B2B 濡쒓렇?꾩썐 湲곕뒫 援ы쁽

## 紐⑺몴
- ?꾨줎?몄뿏?? 湲濡쒕쾶 ?ㅻ퉬寃뚯씠??濡쒓렇?꾩썐 踰꾪듉 (lucide-react LogOut ?꾩씠肄?
- ?대┃ ???곹깭(useAppStore) 珥덇린??諛?`/login` 由щ떎?대젆??- 諛깆뿏?? `POST /v1/auth/logout` API
- Redis `RedissonClient` 釉붾옓由ъ뒪??Blacklisting) 援ы쁽
- `JwtInterceptor`?먯꽌 釉붾옓由ъ뒪??寃利?泥섎━ (401 Unauthorized)

## 吏꾪뻾 ?덉뒪?좊━
- 2026-06-11: `AuthController`??logout ?붾뱶?ъ씤??異붽? 諛?`RedissonClient` ?곕룞
- 2026-06-11: `JwtInterceptor`??釉붾옓由ъ뒪???뺤씤 濡쒖쭅 ?묒꽦
- 2026-06-11: `JwtUtil` 留뚮즺 ?쒓컙(TTL) 怨꾩궛 硫붿꽌??`getExpirationFromToken`) 異붽?
- 2026-06-11: ?꾨줎?몄뿏??`api.ts`??`logoutUser` ?몄텧 ?곕룞
- 2026-06-11: ?꾨줎?몄뿏??`Sidebar.tsx`??`LogOut` ?꾩씠肄?異붽? 諛?Zustand ?곹깭 珥덇린??濡쒖쭅 援ы쁽
- 2026-06-11: 諛깆뿏??`src/test/`??TDD 湲곕컲 `AuthControllerTest`, `JwtInterceptorBlacklistTest` ?뚯뒪???듦낵 ?꾨즺.
