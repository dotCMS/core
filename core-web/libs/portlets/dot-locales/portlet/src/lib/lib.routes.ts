import { Route } from '@angular/router';

import { DotLocalesListResolver } from '@dotcms/portlets/dot-locales/portlet/data-access';

import { DotLocalesListComponent } from './dot-locales-list/dot-locales-list.component';
import { DotLocalesShellComponent } from './dot-locales-shell/dot-locales-shell.component';

export const DotLocalesRoutes: Route[] = [
    {
        path: '',
        component: DotLocalesShellComponent,
        children: [
            {
                path: '',
                component: DotLocalesListComponent,
                resolve: { locales: DotLocalesListResolver }
            }
        ]
    }
];
