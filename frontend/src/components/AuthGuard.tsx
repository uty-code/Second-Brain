'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAppStore } from '@/store/useAppStore';

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { isLoggedIn } = useAppStore();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted) {
      if (!isLoggedIn && pathname !== '/login') {
        router.push('/login');
      } else if (isLoggedIn && pathname === '/login') {
        router.push('/');
      }
    }
  }, [isLoggedIn, pathname, router, mounted]);

  // Avoid hydration mismatch by not rendering until mounted
  if (!mounted) return null;

  // 리다이렉션 진행 중에는 자식 컴포넌트 렌더링을 차단하여 불필요한 API 호출(403) 방어
  if (!isLoggedIn && pathname !== '/login') {
    return null;
  }
  if (isLoggedIn && pathname === '/login') {
    return null;
  }

  return <>{children}</>;
}
