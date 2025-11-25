"use client";

import WebPageContent from "./content-types/webPageContent";
import Banner from "./content-types/banner";
import Activity from "./content-types/activity";
import CallToAction from "./content-types/callToAction";
import CalendarEvent from "./content-types/calendarEvent";
import Product from "./content-types/product";
import ImageComponent from "./content-types/image";

import Header from "./layout/header";
import Footer from "./layout/footer/footer";
import Navigation from "./layout/navigation";
import { usePathname, useRouter } from "next/navigation";
import { DotcmsLayout } from "@dotcms/react";
import { withExperiments } from "@dotcms/experiments";
import { CustomNoComponent } from "./content-types/empty";

import { usePageAsset } from "../hooks/usePageAsset";
import BlogWithBlockEditor from "./content-types/blog";
import { DotCmsClient } from "@dotcms/client";

/**
 * Configure experiment settings below. If you are not using experiments,
 * you can ignore or remove the experiment-related code and imports.
 */
const experimentConfig = {
    apiKey: process.env.NEXT_PUBLIC_EXPERIMENTS_API_KEY, // API key for experiments, should be securely stored
    server: process.env.NEXT_PUBLIC_DOTCMS_HOST, // DotCMS server endpoint
    debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG, // Debug mode for additional logging
};

// Mapping of components to DotCMS content types
const componentsMap = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent,
    BlockEditorItem: BlogWithBlockEditor,
};

export function MyPage({ pageAsset, nav }) {
    const { replace } = useRouter();
    const pathname = usePathname();

    /**
     * If using experiments, `DotLayoutComponent` is `withExperiments(DotcmsLayout)`.
     * If not using experiments:
     * - Replace the below line with `const DotLayoutComponent = DotcmsLayout;`
     * - Remove DotExperimentsProvider from the return statement.
     */
    const DotLayoutComponent = experimentConfig.apiKey
        ? withExperiments(DotcmsLayout, {
              ...experimentConfig,
              redirectFn: replace,
          })
        : DotcmsLayout;

    pageAsset = usePageAsset(pageAsset);

    return (
        <div className="flex flex-col gap-6 min-h-screen bg-lime-50">
            {pageAsset.layout.header && (
                <Header>
                    <Navigation items={nav} />
                </Header>
            )}

            <main className="flex flex-col gap-8 m-auto">
                <DotLayoutComponent
                    pageContext={{
                        components: componentsMap,
                        pageAsset: pageAsset,
                    }}
                    config={{
                        pathname,
                        editor: {
                            params: {
                                depth: 3,
                            },
                        },
                    }}
                />
            </main>

            {pageAsset.layout.footer && <Footer />}
        </div>
    );
}
