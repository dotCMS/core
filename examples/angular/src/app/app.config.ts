import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import {
  DotCMSEditablePageService,
  provideDotCMSClient,
  provideDotCMSImageLoader,
} from '@dotcms/angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    /**
     * We provide the DotCMSClient instance, enabling
     * its injection throughout the application. This approach ensures a single DotCMSClient
     * instance is used, promoting consistency and centralized management of client configuration.
     */
    provideDotCMSClient({
      dotcmsUrl: environment.dotcmsUrl,
      authToken: environment.authToken,
      siteId: environment.siteId,
    }),
    /**
     * This custom image loader, designed for the NgOptimizedImage component, appends the dotCMS URL
     * to the image source if it’s not an external URL.
     *
     * Additionally, it appends the ⁠language_id query parameter if the ⁠loaderParams object contains
     * a ⁠languageId key. To use an image from an external URL, set the ⁠isOutsideSRC key to ⁠true in
     * the ⁠loaderParams object.
     * <img [ngSrc]="https://my-url.com/some.jpg" [loaderParams]="{isOutsideSRC: true}" />
     * For further customization, you can provide your own image loader implementation.
     */
    provideDotCMSImageLoader(environment.dotcmsUrl),
    DotCMSEditablePageService,
  ],
};
