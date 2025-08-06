// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import {
    provideHttpClient,
    HttpClient,
    withFetch,
    withInterceptorsFromDi,
} from '@angular/common/http';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import {
    DotCMSClient,
    DotCMSEditablePageService,
    provideDotCMSImageLoader,
} from '@dotcms/angular';
import {
    provideClientHydration,
    withEventReplay,
} from '@angular/platform-browser';
import { AngularHttpClient } from './angular-httpclient';
import { createDotCMSClient } from '@dotcms/client';

// Simple provider for DotCMSClient
function provideDotCMSClient(config: any) {
    return {
        provide: DotCMSClient,
        useFactory: (httpClient: HttpClient) => {
            const angularHttpClient = new AngularHttpClient(httpClient);
            const dotCMSClient = createDotCMSClient({
                dotcmsUrl: config.dotcmsUrl,
                authToken: config.authToken,
                siteId: config.siteId,
                httpClient: angularHttpClient,
            });
            return new DotCMSClient(dotCMSClient);
        },
        deps: [HttpClient],
    };
}

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        // HTTP Client with proper configuration for SSR
        provideHttpClient(withFetch(), withInterceptorsFromDi()),
        // DotCMS Client - simplified provider
        provideDotCMSClient({
            dotcmsUrl: environment.dotcmsUrl,
            authToken: environment.authToken,
            siteId: environment.siteId,
        }),
        provideDotCMSImageLoader(environment.dotcmsUrl),
        DotCMSEditablePageService,
        // Client Hydration - TransferState is handled manually in AngularHttpClient
        provideClientHydration(
            withEventReplay()
            // Note: We don't need withHttpTransferCacheOptions since we're manually
            // managing TransferState in our custom AngularHttpClient
        ),
    ],
};
