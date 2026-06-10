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

  return <>{children}</>;
}
