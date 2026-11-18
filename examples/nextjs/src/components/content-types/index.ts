import dynamic from "next/dynamic";

import { CustomNoComponent } from "./Empty";

// Keys must match the Content Type variable name in dotCMS. The object literal
// is passed directly to `DotCMSLayoutBody`'s `components` prop, which infers a
// compatible shape — no explicit annotation needed.
//
// Each component is wrapped in `next/dynamic` so it becomes its own chunk, fetched
// only when a page actually contains that content type. Importing them statically
// put every mapped component into the client bundle of every route, whether or not
// the page used them — on this site that is a dozen components for a page that
// typically renders two or three.
//
// `CustomNoComponent` stays eagerly imported on purpose: it is the fallback for
// unmatched content types, and it should render without waiting on a network
// round-trip. It is also tiny.
//
// `next/dynamic` brings its own Suspense boundary. When you map lazy components in
// a framework that does not (plain `React.lazy`, as the Astro example does), the
// dotCMS React SDK wraps each contentlet in a Suspense boundary for you.
export const pageComponents = {
    Activity: dynamic(() => import("./Activity")),
    Banner: dynamic(() => import("./Banner")),
    BannerCarousel: dynamic(() => import("./BannerCarousel")),
    calendarEvent: dynamic(() => import("./CalendarEvent")),
    CallToAction: dynamic(() => import("./CallToAction")),
    CategoryFilter: dynamic(() => import("./CategoryFilter")),
    CustomNoComponent: CustomNoComponent,
    Image: dynamic(() => import("./Image")),
    Product: dynamic(() => import("./Product")),
    SimpleWidget: dynamic(() => import("./SimpleWidget")),
    StoreProductList: dynamic(() => import("./StoreProductList")),
    UnusedComponentProbe: dynamic(() => import("./UnusedComponentProbe")),
    VtlInclude: dynamic(() => import("./VtlInclude")),
    webPageContent: dynamic(() => import("./WebPageContent")),
    YouTube: dynamic(() => import("./YouTube")),
};
