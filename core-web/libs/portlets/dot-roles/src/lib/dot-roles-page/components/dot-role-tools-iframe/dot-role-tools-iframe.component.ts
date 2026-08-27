import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesStore } from '../../store/dot-roles.store';

/** Base path of the wrapper JSP that renders the Dojo tool groups widget. */
const TOOLS_WRAPPER_JSP = '/html/portlet/ext/roleadmin/view_role_tools_wrapper.jsp';

/** Same shape the JSP validates against — see permissions iframe for the rationale. */
const ROLE_ID_UUID =
    /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/;

/**
 * Tools tab hosts an iframe wrapping the existing Dojo Tool Groups UI.
 * Interim per epic #36909 until the Angular replacement ships. URL
 * construction mirrors the permissions iframe: `encodeURIComponent` on the
 * role id then `bypassSecurityTrustResourceUrl` on a same-origin path.
 */
@Component({
    selector: 'dot-role-tools-iframe',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-role-tools-iframe.component.html',
    host: { class: 'block h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRoleToolsIframeComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #sanitizer = inject(DomSanitizer);

    protected readonly $iframeUrl = computed<SafeResourceUrl | null>(() => {
        const roleId = this.store.selectedRoleId();
        if (!roleId || !ROLE_ID_UUID.test(roleId)) {
            return null;
        }

        const url = `${TOOLS_WRAPPER_JSP}?roleId=${encodeURIComponent(roleId)}`;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });
}
