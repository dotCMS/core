import { Route } from '@angular/router';

import { PageComponent } from './dotcms/pages/page/page.component';

export const appRoutes: Route[] = [{
    path: '**',
    component: PageComponent
}];
