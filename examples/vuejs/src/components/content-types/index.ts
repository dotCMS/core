import type { Component } from 'vue';

import Activity from './Activity.vue';
import Banner from './Banner.vue';
import BannerCarousel from './BannerCarousel.vue';
import CalendarEvent from './CalendarEvent.vue';
import CallToAction from './CallToAction.vue';
import CategoryFilter from './CategoryFilter.vue';
import Empty from './Empty.vue';
import ImageComponent from './Image.vue';
import PageForm from './PageForm.vue';
import Product from './Product.vue';
import SimpleWidget from './SimpleWidget.vue';
import StoreProductList from './StoreProductList.vue';
import VtlInclude from './VtlInclude.vue';
import WebPageContent from './WebPageContent.vue';
import YouTube from './YouTube.vue';

/**
 * Maps dotCMS Content Type variable names to the Vue component that renders them.
 * Keys MUST match the Content Type variable name in dotCMS exactly (note the
 * lowercase `calendarEvent` and `webPageContent`). `CustomNoComponent` is the
 * fallback rendered when no mapping exists.
 */
export const pageComponents: Record<string, Component> = {
    Activity,
    Banner,
    BannerCarousel,
    calendarEvent: CalendarEvent,
    CallToAction,
    CategoryFilter,
    CustomNoComponent: Empty,
    Image: ImageComponent,
    PageForm,
    Product,
    SimpleWidget,
    StoreProductList,
    VtlInclude,
    webPageContent: WebPageContent,
    YouTube
};
