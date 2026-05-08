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
                canDeactivate: [unsavedChangesGuard],
                // Opt out of DotCustomReuseStrategyService so locale switch and
                // version restore (which navigate `:id` → `:id`) destroy/recreate
                // the component. Without this, `canDeactivate` does not fire on
                // same-route navigation and the form would silently rebind to
                // the new contentlet without prompting the user.
                data: { reuseRoute: false }
            },
            {
                path: ':id',
                title: 'Edit Content',
                component: DotEditContentLayoutComponent,
                canDeactivate: [unsavedChangesGuard],
                data: { reuseRoute: false }
            }
        ]
    }
];
