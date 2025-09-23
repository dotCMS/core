import { Routes } from '@angular/router';
import { PageComponent } from './dotcms/pages/page/page';

export const routes: Routes = [{
  path: '**',
  component: PageComponent,
}];
