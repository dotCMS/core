import { Route } from '@angular/router';

import { EditContentShellComponent } from './edit-content.shell.component';
import { EditContentLayoutComponent } from './feature/edit-content/edit-content.layout.component';

export const DotEditContentRoutes: Route[] = [
    {
        path: '',
        component: EditContentShellComponent,
        children: [
            {
                path: 'new/:contentType',
                title: 'Create Content',
                component: EditContentLayoutComponent
            },
            { path: ':id', title: 'Edit Content', component: EditContentLayoutComponent }
        ]
    }
];
