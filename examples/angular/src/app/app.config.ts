import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { ClientConfig } from '@dotcms/client';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { provideDotCMSClient } from './lib/dotcms-client-token';
import { PageContextService } from './lib/services/dotcms-context/page-context.service';
import { DotCMSPageResolverService } from '@dotcms/angular';

// import { provideDotCMSClient } from './lib/dotcms-client-token';
// import { PageContextService } from './lib/services/dotcms-context/page-context.service';

// import { PageContextService, provideDotCMSClient } from '@dotcms/angular'

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(routes),
        provideDotCMSClient(DOTCMS_CLIENT_CONFIG),
        PageContextService,
        DotCMSPageResolverService
    ],
};
