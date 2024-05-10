import WebPageContent from './webPageContent';
import Banner from './banner';
import Activity from './activity';
import Product from './product';
import ImageComponent from './image';

// Provide a component for each content type
export const contentComponents = {
    webPageContent: WebPageContent,
    Banner: Banner,
    Activity: Activity,
    Product: Product,
    Image: ImageComponent
};
