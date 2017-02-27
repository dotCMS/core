import {AppModule} from './components/app.module';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {enableProdMode} from '@angular/core';
import {CONSTANTS} from './constants';

if (CONSTANTS.ENV === 'PROD') {
    enableProdMode();
}

const platform = platformBrowserDynamic();

platform.bootstrapModule(AppModule);