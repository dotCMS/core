import { ApplicationConfig, EnvironmentProviders, inject, makeEnvironmentProviders } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, HttpClient, withFetch } from '@angular/common/http';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { DotCMSClient, DotCMSEditablePageService, provideDotCMSImageLoader } from '@dotcms/angular';
import { provideClientHydration, withEventReplay, withHttpTransferCacheOptions } from '@angular/platform-browser';
import { AngularHttpClient } from './angular-httpclient';
import { DotCMSClientConfig } from '@dotcms/types';
import { createDotCMSClient } from '@dotcms/client';

function provideDotCMSClient(options: any): EnvironmentProviders {

  return makeEnvironmentProviders([
      {
          provide: DotCMSClient,
          useFactory: () => {
            const ngHttpclient = inject(HttpClient)
            const dotCMSClient = createDotCMSClient({
              dotcmsUrl: options.dotcmsUrl,
              authToken: options.authToken,
              siteId: options.siteId,
              httpClient: new options.httpClient(ngHttpclient)
            });

            return new DotCMSClient(dotCMSClient);
          }
      }
  ]);
}


export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        provideHttpClient(withFetch()),
        /**
         * We provide the DotCMSClient instance, enabling
         * its injection throughout the application. This approach ensures a single DotCMSClient
         * instance is used, promoting consistency and centralized management of client configuration.
         */
        provideDotCMSClient({
          dotcmsUrl: environment.dotcmsUrl,
          authToken: environment.authToken,
          siteId: environment.siteId,
          httpClient: AngularHttpClient
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
        provideClientHydration(withEventReplay(), withHttpTransferCacheOptions({}))
    ]
};
