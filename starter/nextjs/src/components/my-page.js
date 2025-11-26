"use client";

import React from "react";
import { usePathname } from "next/navigation";
import { DotcmsLayout } from "@dotcms/react";
import { usePageAsset } from "@/hooks/usePageAsset";
import DummyContentlet from "./dummy";

/**
 * Components mapping for DotCMS content types.
 * Add your custom components here using the content type variable name as the key.
 *
 * Example:
 * {
 *   "Banner": BannerComponent,
 *   "BlogPost": BlogPostComponent,
 *   "ProductCard": ProductCardComponent
 * }
 *
 * Any unmapped content types will fallback to the DummyContentlet component
 * which displays the raw content structure.
 */
const components = {
};

const componentsMap = new Proxy(components, {
  get: (target, prop) =>
    target[prop] ||
    ((contentlet) => <DummyContentlet data={contentlet} />),
});

export function MyPage({ pageAsset }) {
    const pathname = usePathname();

    pageAsset = usePageAsset(pageAsset);

    return (
        <DotcmsLayout
            pageContext={{
                pageAsset,
                components: componentsMap,
            }}
            config={{
                pathname,
                editor: {
                    params: {
                        depth: 1,
                    },
                },
            }}
        />
    );
}
