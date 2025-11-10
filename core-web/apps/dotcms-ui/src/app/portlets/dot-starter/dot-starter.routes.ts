import { Routes } from '@angular/router';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterComponent } from './dot-starter.component';

export const dotStarterRoutes: Routes = [
    {
        component: DotStarterComponent,
        path: '',
        resolve: {
            userData: DotStarterResolver
        }
    }
];
