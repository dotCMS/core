import { Route } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DotUsageShellComponent } from './lib/dot-usage-shell/dot-usage-shell.component';

export const DotUsageRoutes: Route[] = [
    {
        path: '',
        component: DotUsageShellComponent,
        providers: [GlobalStore, DotContentTypeService]
    }
];
