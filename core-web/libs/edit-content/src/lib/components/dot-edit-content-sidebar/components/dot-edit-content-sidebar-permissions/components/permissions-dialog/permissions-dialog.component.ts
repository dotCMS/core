import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

export const PERMISSIONS_IFRAME_PATH = '/html/portlet/ext/contentlet/permissions.jsp';

export interface PermissionsDialogData {
    identifier: string;
    languageId: number;
}

function buildPermissionsIframeUrl(identifier: string, languageId: number): string {
    const params = new URLSearchParams({
        contentletId: identifier,
        languageId: String(languageId),
        popup: 'true',
        in_frame: 'true',
        frame: 'detailFrame',
        container: 'true',
        angularCurrentPortlet: 'edit-content'
    });
    return `${PERMISSIONS_IFRAME_PATH}?${params.toString()}`;
}

/**
 * Dialog component that displays contentlet permissions in an iframe.
 * Used from the Permissions tab in the edit content sidebar.
 */
@Component({
    selector: 'dot-permissions-dialog',
    templateUrl: './permissions-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPermissionsDialogComponent {
    readonly #config = inject(DynamicDialogConfig<PermissionsDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    readonly #identifier = this.#config.data?.identifier ?? '';
    readonly #languageId = this.#config.data?.languageId ?? 0;

    readonly iframeSrc = computed<SafeResourceUrl | null>(() => {
        const identifier = this.#identifier;
        const languageId = this.#languageId;
        if (!identifier || !languageId) return null;
        return this.#sanitizer.bypassSecurityTrustResourceUrl(
            buildPermissionsIframeUrl(identifier, languageId)
        );
    });
}
