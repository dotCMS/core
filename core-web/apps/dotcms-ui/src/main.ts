import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from '@dotcms/app/app.module';
import { environment } from './environments/environment';
import { defineCustomElements } from '@dotcms/dotcms-webcomponents/loader';

if (environment.production) {
    enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule);
defineCustomElements();
