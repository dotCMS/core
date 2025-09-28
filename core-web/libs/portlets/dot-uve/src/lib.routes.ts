import { Route } from '@angular/router';

import { DotUVEContentComponent } from './dot-uve-content/dot-uve-content.component';
import { DotUVEShellComponent } from './dot-uve-shell/dot-uve-shell.component';

export const dotUVERoutes: Route[] = [
    {
        path: '',
        component: DotUVEShellComponent,
        children: [
            {
                path: 'content',
                component: DotUVEContentComponent
            }
        ]
    }
];
