import { Route } from '@angular/router';

import { DotLanguagesListResolver } from '@dotcms/portlets/dot-languages/portlet/data-access';

import { DotLanguagesListComponent } from './dot-languages-list/dot-languages-list.component';
import { DotLanguagesShellComponent } from './dot-languages-shell/dot-languages-shell.component';

export const DotLanguagesRoutes: Route[] = [
    {
        path: '',
        component: DotLanguagesShellComponent,
        children: [
            {
                path: '',
                component: DotLanguagesListComponent,
                resolve: { languages: DotLanguagesListResolver }
            }
        ]
    }
];
