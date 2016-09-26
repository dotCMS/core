import {AppModule} from './components/app.module';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';

const platform = platformBrowserDynamic();

platform.bootstrapModule(AppModule);