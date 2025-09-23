import { Routes } from '@angular/router';
import { Page } from './dotcms/pages/page/page';

export const routes: Routes = [{
  path: '**',
  component: Page,
}];
