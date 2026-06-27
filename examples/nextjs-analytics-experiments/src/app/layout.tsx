import { DotContentAnalytics } from "@dotcms/analytics/react";
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";

import { AnalyticsDemoCta } from "@/components/AnalyticsDemoCta";
import { analyticsConfig } from "@/config/dotcms.config";

import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "dotCMS Analytics + Experiments",
  description:
    "Minimal Next.js example with dotCMS Content Analytics and A/B Experiments",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col">
        <DotContentAnalytics config={analyticsConfig} />
        <header className="border-b border-zinc-200 px-6 py-3 dark:border-zinc-800">
          <div className="container mx-auto flex items-center justify-between gap-4">
            <nav className="flex gap-4 text-sm font-medium">
              <Link href="/" className="hover:underline">
                Home
              </Link>
              <Link href="/blog" className="hover:underline">
                Blog (Experiments)
              </Link>
            </nav>
            <AnalyticsDemoCta />
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
