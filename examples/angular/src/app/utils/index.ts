export const COMPONENTS: Record<string, any> = {
  Activity: {
    // TODO: Discuss with the team if we should use the `import` function here.
    component: import('../dotcms-pages/content-types/activity/activity.component').then(c => c.ActivityComponent),
  },
  Banner: {
    component: import('../dotcms-pages/content-types/banner/banner.component').then(c => c.BannerComponent),
  },
  Image: {
    component: import('../dotcms-pages/content-types/image/image.component').then(c => c.ImageComponent),
  },
  webPageContent: {
    component: import('../dotcms-pages/content-types/web-page-content/web-page-content.component').then(c => c.WebPageContentComponent),
  },
  Product: {
    component: import('../dotcms-pages/content-types/product/product.component').then(c => c.ProductComponent),
  }
};