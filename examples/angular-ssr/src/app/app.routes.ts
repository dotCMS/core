import { Routes } from '@angular/router';
import { App } from './app';

export const routes: Routes = [{
  path: '**',
  component: App,
}];
