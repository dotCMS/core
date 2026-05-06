import { Route } from '@angular/router';

import { DotEditContentLayoutComponent } from './components/dot-edit-content-layout/dot-edit-content.layout.component';
import { EditContentShellComponent } from './edit-content.shell.component';
import { unsavedChangesGuard } from './guards/unsaved-changes.guard';

export const dotEditContentRoutes: Route[] = [
    {
        path: '',
        component: EditContentShellComponent,
        children: [
            {
                path: 'new/:contentType',
                title: 'Create Content',
                component: DotEditContentLayoutComponent,
                canDeactivate: [unsavedChangesGuard]
            },
            {
                path: ':id',
                title: 'Edit Content',
                component: DotEditContentLayoutComponent,
                canDeactivate: [unsavedChangesGuard]
            }
        ]
    }
];
