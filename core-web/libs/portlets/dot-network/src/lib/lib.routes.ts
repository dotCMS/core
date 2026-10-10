import { Route } from '@angular/router';

import { DotNetworkShellComponent } from './dot-network-shell/dot-network-shell.component';

export const dotNetworkRoutes: Route[] = [
    {
        path: '',
        component: DotNetworkShellComponent
    }
];
