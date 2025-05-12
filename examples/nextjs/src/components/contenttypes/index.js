import { CustomNoComponent } from './Empty';
import Activity from './Activity';
import Banner from './Banner';
import Blog from './Blog';
import CalendarEvent from './CalendarEvent';
import CallToAction from './CallToAction';
import ImageComponent from './Image';
import Product from './Product';
import WebPageContent from './WebPageContent';

// Provide a component for each content type
export const contentComponents = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent
};

// Mapping of components to DotCMS content types
export const pageComponents = {
    Blog: Blog,
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent,
    calendarEvent: CalendarEvent,
    CallToAction: CallToAction,
    CustomNoComponent: CustomNoComponent
};