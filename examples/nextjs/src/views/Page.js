"use client";

import { DotCMSLayoutBody, useEditableDotCMSPage } from "@dotcms/react";

import { pageComponents } from "@/components/content-types";
import Footer from "@/components/footer/Footer";
import Header from "@/components/Header";

import { DotContentAnalyticsProvider } from "@dotcms/analytics/react";

const analyticsConfig = {
    siteKey: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY || 'KEY_GENERATED_WITH_ANALYTICS_APP_AT_DOTCMS',
    server: process.env.NEXT_PUBLIC_DOTCMS_HOST || 'http://localhost:8080',
    debug: 'prod'
};

export function Page({ pageContent }) {
    const { pageAsset, content = {} } = useEditableDotCMSPage(pageContent);
    const navigation = content.navigation;

    return (
        <DotContentAnalyticsProvider config={analyticsConfig}>
            <div className="flex flex-col gap-6 min-h-screen bg-slate-50">
                {pageAsset?.layout.header && (
                    <Header navItems={navigation?.children} />
                )}

                <main className="container m-auto">
                    <DotCMSLayoutBody
                        page={pageAsset}
                        components={pageComponents}
                        mode={process.env.NEXT_PUBLIC_DOTCMS_MODE}
                    />
                </main>

                {pageAsset?.layout.footer && <Footer {...content} />}
            </div>
        </DotContentAnalyticsProvider>
    );
}
