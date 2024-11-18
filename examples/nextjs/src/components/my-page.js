"use client";
import { usePathname } from "next/navigation";
import { DotcmsLayout } from "@dotcms/react";

import WebPageContent from "./content-types/webPageContent";
import Banner from "./content-types/banner";
import Activity from "./content-types/activity";
import CallToAction from "./content-types/callToAction";
import CalendarEvent from "./content-types/calendarEvent";
import Product from "./content-types/product";
import ImageComponent from "./content-types/image";
import { Video } from "./content-types/Video";
import BlogWithBlockEditor from "./content-types/blog";
import { CustomNoComponent } from "./content-types/empty";
import { SimpleWidget } from "./content-types/SimpleWidget";

import Header from "./layout/header";
import Footer from "./layout/footer/footer";
import Navigation from "./layout/navigation";

import NotFound from "@/app/not-found";
import { usePageAsset } from "../hooks/usePageAsset";

// Mapping of components to DotCMS content types
const components = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent,
    BlockEditorItem: BlogWithBlockEditor,
    SimpleWidget: SimpleWidget,
    Video: Video
};

const componentsMap = new Proxy(components, {
    get: (target, prop) =>
        target[prop] ||
        ((contentlet) => <div>{contentlet.contentType}</div>),
});

export function MyPage({ pageAsset, nav }) {
    const pathname = usePathname();

    pageAsset = usePageAsset(pageAsset);

    if (!pageAsset) {
        return <NotFound />;
    }

    return (
        <div className="flex flex-col min-h-screen gap-6 bg-lime-50">
            {pageAsset?.layout.header && (
                <Header>{!!nav && <Navigation items={nav} />}</Header>
            )}

            <main>
                <DotcmsLayout
                    pageContext={{
                        pageAsset,
                        components: componentsMap
                    }}
                    config={{
                        pathname,
                        editor: {
                            params: {
                                depth: 3
                            }
                        }
                    }}
                />
            </main>

            {pageAsset?.layout.footer && <Footer />}
        </div>
    );
}
