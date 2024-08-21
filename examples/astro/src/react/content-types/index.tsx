import Activity from "./activity";
import Banner from "./banner";
import CalendarEvent from "./calendarEvent";
import CallToAction from "./callToAction";
import { CustomNoComponent } from "./empty";
import ImageComponent from "./image";
import Product from "./product";
import WebPageContent from "./webPageContent";


// Mapping of components to DotCMS content types
export default {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent,
}