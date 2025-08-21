import { Route } from '@angular/router';

import { Page } from './page';
import { PageNative } from './page.native';

export const appRoutes: Route[] = [
    {
        path: '',
        component: Page
    },
    {
        path: 'native',
        component: PageNative
    }
];
