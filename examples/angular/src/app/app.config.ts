import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';

import { ClientConfig } from '@dotcms/client';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { provideDotcmsClient } from '@dotcms/angular';

const DOTCMS_CLIENT_CONFIG: ClientConfig = {
    dotcmsUrl: environment.dotcmsUrl,
    authToken: environment.authToken,
    siteId: environment.siteId
};

export const appConfig: ApplicationConfig = {
    providers: [
        provideDotcmsClient(DOTCMS_CLIENT_CONFIG),
        provideRouter(routes),
    ],
};
