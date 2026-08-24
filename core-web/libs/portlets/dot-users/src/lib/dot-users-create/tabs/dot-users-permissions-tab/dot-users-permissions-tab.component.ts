import { Component, computed, input } from '@angular/core';

import { DotMessagePipe, DotPermissionsIframeComponent } from '@dotcms/ui';

/**
 * Permissions tab. Renders the legacy Dojo role-permissions UI inside an
 * iframe until the native Angular implementation against
 * GET/PUT /api/v1/permissions/user/{userId} lands.
 *
 * The URL points at `/html/portlet/ext/useradmin/permissions.jsp`, a thin
 * wrapper JSP that resolves the user's implicit role and delegates to the
 * shared `view_role_permissions_inc.jsp` fragment. `popup=true` is
 * mandatory — without it the legacy `top_inc.jsp` renders
 * `<body style="visibility:hidden">` and the iframe shows up blank.
 *
 * In create mode the userId does not exist yet, so we render an empty
 * state prompting the user to save first.
 */
@Component({
    selector: 'dot-users-permissions-tab',
    imports: [DotMessagePipe, DotPermissionsIframeComponent],
    templateUrl: './dot-users-permissions-tab.component.html',
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotUsersPermissionsTabComponent {
    readonly userId = input<string | null>(null);

    readonly $permissionsUrl = computed<string>(() => {
        const id = this.userId();
        if (!id) {
            return '';
        }
        const params = new URLSearchParams({ userId: id, popup: 'true' });

        return `/html/portlet/ext/useradmin/permissions.jsp?${params.toString()}`;
    });
}
