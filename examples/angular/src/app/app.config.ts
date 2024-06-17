import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { ClientConfig } from '@dotcms/client';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { provideDotcmsClient } from './client-token/dotcms-client';
import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
  dotcmsUrl: environment.dotcmsUrl,
  authToken: environment.authToken,
  siteId: environment.siteId,
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideDotcmsClient(DOTCMS_CLIENT_CONFIG),
    provideRouter(routes),
    /**
     * This is a custom image loader that will be used by the NgOptimizedImage component.
     * It will prepend the dotCMS URL to the image src if the image is not an external URL.
     * It will also append the language_id query parameter if the loaderParams object contains a languageId key.
     * If you need to use an imagen from an external URL, you can set the isOutsideSRC key to true in the loaderParams object.
     * <img [ngSrc]="https://my-url.com/some.jpg" [loaderParams]="{isOutsideSRC: true}" />
     * If you need to customize the image loader, you can provide your own implementation.
     */
    {
      provide: IMAGE_LOADER,
      useValue: (config: ImageLoaderConfig) => {
        let url = config.loaderParams?.['isOutsideSRC']
          ? config.src
          : `${environment.dotcmsUrl}${config.src}`;
        if (config.loaderParams?.['languageId']) {
          url = `${url}/${config.width}?language_id=${config.loaderParams['languageId']}`;
        }
        return url;
      },
    },
  ],
};
