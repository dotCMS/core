import { DynamicComponentEntity } from '@dotcms/angular';

export const DYNAMIC_COMPONENTS: { [key: string]: DynamicComponentEntity } = {
  Activity: import('../content-types/activity/activity.component').then(
    (c) => c.ActivityComponent,
  ),
  Banner: import('../content-types/banner/banner.component').then(
    (c) => c.BannerComponent,
  ),
  Image: import('../content-types/image/image.component').then(
    (c) => c.ImageComponent,
  ),
  webPageContent: import(
    '../content-types/web-page-content/web-page-content.component'
  ).then((c) => c.WebPageContentComponent),
  Product: import('../content-types/product/product.component').then(
    (c) => c.ProductComponent,
  ),
  CustomNoComponent: import(
    '../content-types/custom-no-component/custom-no-component.component'
  ).then((c) => c.CustomNoComponent),
  BannerCarousel: import(
    '../content-types/banner-carousel/banner-carousel.component'
  ).then((c) => c.BannerCarouselComponent),
  VtlInclude: import('../content-types/vtl-include/vtl-include.component').then(
    (c) => c.VtlIncludeComponent,
  ),
  CategoryFilter: import(
    '../content-types/category-filter/category-filter.component'
  ).then((c) => c.CategoryFilterComponent),
  StoreProductList: import(
    '../content-types/store-product-list/store-product-list.component'
  ).then((c) => c.StoreProductListComponent),
  SimpleWidget: import(
    '../content-types/simple-widget/simple-widget.component'
  ).then((c) => c.SimpleWidgetComponent),
  PageForm: import('../content-types/page-form/page-form.component').then(
    (c) => c.PageFormComponent,
  ),
};
