import { Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesStore } from '../../store/dot-roles.store';

/**
 * Permissions tab hosts an iframe wrapping the existing Dojo permissions UI.
 *
 * Interim solution per epic #36909 — the Angular replacement is tracked as
 * a separate follow-up task filed off the spike #36929. The wrapper JSP at
 * `view_role_permissions_wrapper.jsp` is created alongside this component in
 * the backend PR and communicates readiness / size via `postMessage`.
 */
@Component({
    selector: 'dot-role-permissions-iframe',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-role-permissions-iframe.component.html',
    host: { class: 'block h-full min-h-[500px]' }
})
export class DotRolePermissionsIframeComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #sanitizer = inject(DomSanitizer);

    protected readonly $iframeUrl = computed<SafeResourceUrl | null>(() => {
        const roleId = this.store.selectedRoleId();
        if (!roleId) {
            return null;
        }

        return this.#sanitizer.bypassSecurityTrustResourceUrl(
            `/html/portlet/ext/roleadmin/view_role_permissions_wrapper.jsp?roleId=${roleId}`
        );
    });
}
