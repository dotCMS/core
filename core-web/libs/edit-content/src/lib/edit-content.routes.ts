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
                canDeactivate: [unsavedChangesGuard]
                // No `reuseRoute: false` here (unlike `new/:contentType`): the editor
                // REUSES its component across `:id → :id` navigations (breadcrumb,
                // locale switch, version restore, related content) so the previous
                // content stays on screen until the new one loads instead of blanking.
                // The layout re-runs initialization from the URL via
                // `host.identityChanges$`. Because a reused route no longer triggers
                // `canDeactivate` on same-route navigation, the unsaved-changes prompt
                // for those in-editor moves is handled at the source by the host's
                // navigation guard. `canDeactivate` still fires when LEAVING the editor
                // (a different route config is not reused), so exiting stays guarded.
            }
        ]
    }
];
