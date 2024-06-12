import { DynamicComponentEntity } from '@dotcms/angular';

export const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
  Activity: import('../pages/content-types/activity/activity.component').then(
    (c) => c.ActivityComponent
  ),
  Banner: import('../pages/content-types/banner/banner.component').then(
    (c) => c.BannerComponent
  ),
  Image: import('../pages/content-types/image/image.component').then(
    (c) => c.ImageComponent
  ),
  webPageContent: import(
    '../pages/content-types/web-page-content/web-page-content.component'
  ).then((c) => c.WebPageContentComponent),
  Product: import('../pages/content-types/product/product.component').then(
    (c) => c.ProductComponent
  ),
  CustomNoComponent: import(
    '../pages/content-types/custom-no-component/custom-no-component.component'
  ).then((c) => c.CustomNoComponent),
};

export type GenericContentlet = {
  urlMap?: string;
  url: string;
  urlTitle?: string;
  image: string;
};
