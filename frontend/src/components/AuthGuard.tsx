'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAppStore } from '@/store/useAppStore';

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { isLoggedIn } = useAppStore();
  const [mounted, setMounted] = useState(false);
  const [hasHydrated, setHasHydrated] = useState(false);

  useEffect(() => {
    setMounted(true);
    useAppStore.persist.onFinishHydration(() => setHasHydrated(true));
    setHasHydrated(useAppStore.persist.hasHydrated());
  }, []);

  useEffect(() => {
    if (mounted && hasHydrated) {
      if (!isLoggedIn && pathname !== '/login') {
        router.push('/login');
      } else if (isLoggedIn && pathname === '/login') {
        router.push('/');
      }
    }
  }, [isLoggedIn, pathname, router, mounted, hasHydrated]);

  // Hydration과 클라이언트 마운트가 모두 완료될 때까지 렌더링 대기
  if (!mounted || !hasHydrated) return null;

  // 리다이렉션 진행 중에는 자식 컴포넌트 렌더링을 차단하여 불필요한 API 호출(403) 방어
  if (!isLoggedIn && pathname !== '/login') {
    return null;
  }
  if (isLoggedIn && pathname === '/login') {
    return null;
  }

  return <>{children}</>;
}
