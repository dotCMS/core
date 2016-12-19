import {AppModule} from './components/app.module';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {enableProdMode} from '@angular/core';
import {CONSTANT} from './constant';

if (CONSTANT.env === 'PROD') {
    enableProdMode();
}

const platform = platformBrowserDynamic();

platform.bootstrapModule(AppModule);