import { Route } from '@angular/router';

import { PageComponent } from './components/page/page.component';

export const appRoutes: Route[] = [{
    path: '**',
    component: PageComponent
}];
