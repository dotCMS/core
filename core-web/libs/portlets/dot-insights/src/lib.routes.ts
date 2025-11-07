import { Route } from '@angular/router';

import { DotContentTypeService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DotInsightsShellComponent } from './lib/dot-insights-shell/dot-insights-shell.component';

export const DotInsightsRoutes: Route[] = [
    {
        path: '',
        component: DotInsightsShellComponent,
        providers: [GlobalStore, DotContentTypeService]
    }
];
