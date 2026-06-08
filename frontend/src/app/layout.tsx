import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AIMS-Graph",
  description: "Second Brain Knowledge Graph Dashboard",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="dark" suppressHydrationWarning>
      <body className="bg-zinc-900 text-zinc-50 antialiased overflow-hidden" suppressHydrationWarning>
        {children}
      </body>
    </html>
  );
}
