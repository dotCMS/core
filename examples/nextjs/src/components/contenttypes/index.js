import { CustomNoComponent } from './empty';
import Activity from './activity';
import Banner from './banner';
import Blog from './blog';
import CalendarEvent from './calendarEvent';
import CallToAction from './callToAction';
import ImageComponent from './image';
import Product from './product';
import WebPageContent from './webPageContent';

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