import { Inter } from "next/font/google";
import { DotContentAnalytics } from "@dotcms/analytics/react";
import { analyticsConfig } from "@/config/analytics.config";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export default function RootLayout({ children }) {
    return (
        <html lang="en">
            <body className={inter.className}>
                <DotContentAnalytics config={analyticsConfig} />
                {children}
            </body>
        </html>
    );
}
