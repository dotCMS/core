import { Routes } from '@angular/router';

import { DotContainerCreateComponent } from './dot-container-create.component';
import { DotContainerEditResolver } from './resolvers/dot-container-edit.resolver';

export const dotContainerCreateRoutes: Routes = [
    {
        path: '',
        component: DotContainerCreateComponent,
        providers: [DotContainerEditResolver]
    }
];
