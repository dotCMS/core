import { Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesStore } from '../../store/dot-roles.store';

/** Base path of the wrapper JSP that renders the Dojo permissions widget. */
const PERMISSIONS_WRAPPER_JSP = '/html/portlet/ext/roleadmin/view_role_permissions_wrapper.jsp';

/**
 * Permissions tab hosts an iframe wrapping the existing Dojo permissions UI.
 *
 * Interim per epic #36909 — the Angular re-implementation of the
 * permissions matrix is out of scope for this Beta. The wrapper JSP
 * (`view_role_permissions_wrapper.jsp`) is intentionally thin: it embeds
 * only the `rolePermissionsWrapper` div and boots the existing Dojo /
 * DWR code path. Swapping this iframe for a native Angular component
 * later is meant to be a drop-in.
 *
 * URL construction follows the same pattern as
 * `DotPermissionsIframeDialogComponent` in `@dotcms/ui`: only same-origin
 * relative paths built from a hard-coded prefix + `encodeURIComponent`d
 * role id are passed through `bypassSecurityTrustResourceUrl`.
 */
@Component({
    selector: 'dot-role-permissions-iframe',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-role-permissions-iframe.component.html',
    host: { class: 'block h-full' }
})
export class DotRolePermissionsIframeComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #sanitizer = inject(DomSanitizer);

    protected readonly $iframeUrl = computed<SafeResourceUrl | null>(() => {
        const roleId = this.store.selectedRoleId();
        if (!roleId) {
            return null;
        }

        const url = `${PERMISSIONS_WRAPPER_JSP}?roleId=${encodeURIComponent(roleId)}`;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });
}
