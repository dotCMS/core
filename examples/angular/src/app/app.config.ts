import { ApplicationConfig, InjectionToken } from '@angular/core';
import { provideRouter } from '@angular/router';
import { IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';

import { ClientConfig, DotCmsClient } from '@dotcms/client';

import { routes } from './app.routes';
import { environment } from '../environments/environment';

export const DOTCMS_CLIENT_TOKEN = new InjectionToken<DotCmsClient>('DOTCMS_CLIENT');

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
  dotcmsUrl: environment.dotcmsUrl,
  authToken: environment.authToken,
  siteId: environment.siteId,
};

const client = DotCmsClient.init(DOTCMS_CLIENT_CONFIG);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    /**
     * We provide the ⁠DOTCMS_CLIENT_TOKEN with the initialized ⁠DotCmsClient instance, enabling
     * its injection throughout the application. This approach ensures a single ⁠DotCmsClient
     * instance is used, promoting consistency and centralized management of client configuration.
     */
    {
      provide: DOTCMS_CLIENT_TOKEN,
      useValue: client
    },
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
        const { loaderParams, src, width } = config;
        const isOutsideSRC = loaderParams?.['isOutsideSRC'];
        if (isOutsideSRC) return src;

        const languageId = loaderParams?.['languageId'];

        const dotcmsHost = new URL(environment.dotcmsUrl).origin;

        const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`;

        return `${dotcmsHost}${imageSRC}/${width}?language_id=${
          languageId || '1'
        }`;
      },
    },
  ],
};
