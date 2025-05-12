"use client";

import NotFound from "@/app/not-found";
import Blog from "@/components/contenttypes/blog";
import Footer from "@/components/layout/footer/footer";
import Header from "@/components/layout/header/header";
import Navigation from "@/components/layout/navigation";

import { useCustomPageAsset } from "@/hooks/useCustomPageAsset";

import { enableBlockEditorInline } from "@dotcms/uve";

export function MyBlogPage({ initialPageAsset, nav }) {
    const pageAsset = useCustomPageAsset(initialPageAsset);
    const { urlContentMap } = pageAsset;

    if (!urlContentMap) {
        return <NotFound />;
    }

    const handleClick = () => {
        enableBlockEditorInline(urlContentMap, "blogContent");
    };

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-lime-50">
            {pageAsset?.layout.header && (
                <Header>{!!nav && <Navigation items={nav} />}</Header>
            )}

            <main className="flex flex-col gap-8 m-auto" onClick={handleClick}>
                <Blog {...urlContentMap} />
            </main>

            {pageAsset?.layout.footer && <Footer />}
        </div>
    );
}
