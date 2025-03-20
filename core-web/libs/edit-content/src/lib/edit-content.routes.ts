import { Route } from '@angular/router';

import { DotEditContentLayoutComponent } from './components/dot-edit-content-layout/dot-edit-content.layout.component';
import { EditContentShellComponent } from './edit-content.shell.component';

export const DotEditContentRoutes: Route[] = [
    {
        path: '',
        component: EditContentShellComponent,
        children: [
            {
                path: 'new/:contentType',
                title: 'Create Content',
                component: DotEditContentLayoutComponent
            },
            {
                path: ':id',
                title: 'Edit Content',
                component: DotEditContentLayoutComponent
            }
        ]
    }
];
