import { Route } from '@angular/router';

import { EditContentShellComponent } from './edit-content.shell.component';
import { FormComponent } from './feature/form/form.component';

export const editContentRoutes: Route[] = [
    {
        path: '',
        component: EditContentShellComponent,
        children: [
            { path: 'new/:contentType', title: 'Create Content', component: FormComponent },
            { path: ':id', title: 'Edit Content', component: FormComponent }
        ]
    }
];
