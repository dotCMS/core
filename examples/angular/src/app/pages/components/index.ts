import { DynamicComponentEntity } from '@dotcms/angular';

export const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
  Activity: import('../../content-types/activity/activity.component').then(
    (c) => c.ActivityComponent
  ),
  Banner: import('../../content-types/banner/banner.component').then(
    (c) => c.BannerComponent
  ),
  Image: import('../../content-types/image/image.component').then(
    (c) => c.ImageComponent
  ),
  webPageContent: import(
    '../../content-types/web-page-content/web-page-content.component'
  ).then((c) => c.WebPageContentComponent),
  Product: import('../../content-types/product/product.component').then(
    (c) => c.ProductComponent
  ),
  CustomNoComponent: import(
    '../../content-types/custom-no-component/custom-no-component.component'
  ).then((c) => c.CustomNoComponent),
};

export type GenericContentlet = {
  urlMap?: string;
  url: string;
  urlTitle?: string;
  image: string;
  widgetTitle?: string;
  onNumberOfPages?: number;
};
