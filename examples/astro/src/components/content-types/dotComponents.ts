import { lazy } from "react";

import { CustomNoComponent } from "./Empty";

// Keys must match the Content Type variable name in dotCMS.
//
// Each component is loaded with `React.lazy` so it becomes its own chunk, fetched only when
// a page actually contains that content type. Importing them statically put every mapped
// component into this island's bundle on every route, whether or not the page used them.
//
// Unlike `next/dynamic`, `React.lazy` does not provide its own Suspense boundary — the
// dotCMS React SDK wraps each contentlet in one, so a lazy component can be mapped here
// directly with no extra wiring.
//
// `CustomNoComponent` stays eagerly imported on purpose: it is the fallback for unmatched
// content types and should render without waiting on a network round-trip.
export const dotComponents = {
  Activity: lazy(() => import("./Activity")),
  Banner: lazy(() => import("./Banner")),
  BannerCarousel: lazy(() => import("./BannerCarousel")),
  CallToAction: lazy(() => import("./CallToAction")),
  CategoryFilter: lazy(() => import("./CategoryFilter")),
  CustomNoComponent,
  PageForm: lazy(() => import("./PageForm")),
  Product: lazy(() => import("./Product")),
  SimpleWidget: lazy(() => import("./SimpleWidget")),
  StoreProductList: lazy(() => import("./StoreProductList")),
  UnusedComponentProbe: lazy(() => import("./UnusedComponentProbe")),
  VtlInclude: lazy(() => import("./VtlInclude")),
  calendarEvent: lazy(() => import("./CalendarEvent")),
  Image: lazy(() => import("./Image")),
  webPageContent: lazy(() => import("./WebPageContent")),
};
