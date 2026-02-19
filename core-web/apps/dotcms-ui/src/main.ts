import { enableProdMode, importProvidersFrom } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { defineCustomElements } from '@dotcms/dotcms-webcomponents/loader';

import { AppComponent } from './app/app.component';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [importProvidersFrom(AppModule, BrowserAnimationsModule)]
});

defineCustomElements();
