import { CustomNoComponent } from "./Empty";
import Activity from "./Activity";
import Banner from "./Banner";
import BannerCarousel from "./BannerCarousel";
import CalendarEvent from "./CalendarEvent";
import CallToAction from "./CallToAction";
import CategoryFilter from "./CategoryFilter";
import SimpleWidget from "./SimpleWidget";
import ImageComponent from "./Image";
import Product from "./Product";
import StoreProductList from "./StoreProductList";
import VtlInclude from "./VtlInclude";
import WebPageContent from "./WebPageContent";
import YouTube from "./YouTube";

// Keys must match the Content Type variable name in dotCMS. The object literal
// is passed directly to `DotCMSLayoutBody`'s `components` prop, which infers a
// compatible shape — no explicit annotation needed.
export const pageComponents = {
    Activity: Activity,
    Banner: Banner,
    BannerCarousel: BannerCarousel,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CategoryFilter: CategoryFilter,
    CustomNoComponent: CustomNoComponent,
    Image: ImageComponent,
    Product: Product,
    SimpleWidget: SimpleWidget,
    StoreProductList: StoreProductList,
    VtlInclude: VtlInclude,
    webPageContent: WebPageContent,
    YouTube: YouTube,
};
