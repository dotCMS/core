"use client";

import NotFound from "@/app/not-found";
import Blog from "@/components/content-types/blog";
import Footer from "@/components/layout/footer/footer";
import Header from "@/components/layout/header/header";
import Navigation from "@/components/layout/navigation";

import { usePageAsset } from "@/hooks/usePageAsset";

export function MyBlogPage({ pageAsset, nav }) {
    pageAsset = usePageAsset(pageAsset);
    
    /**
     * Learn more about urlContentMap at:
     * https://www2.dotcms.com/docs/latest/using-url-mapped-content
     * 
    */
    const { urlContentMap } = pageAsset || {};

    if (!urlContentMap) {
        return <NotFound />;
    }

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-lime-50">
            {pageAsset?.layout.header && (
                <Header>{!!nav && <Navigation items={nav} />}</Header>
            )}

            <main className="flex flex-col gap-8 m-auto">
                <Blog {...urlContentMap} />
            </main>

            {pageAsset?.layout.footer && <Footer />}
        </div>
    );
}
