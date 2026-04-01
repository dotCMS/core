import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotPermissionsIframeDialogData {
    url: string;
    minHeight?: string;
}

/**
 * Generic dialog component that displays any permissions page in an iframe.
 * Callers are responsible for building the URL before opening the dialog.
 *
 * Usage:
 *   dialogService.open(DotPermissionsIframeDialogComponent, {
 *     header: 'Permissions',
 *     data: { url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=...' }
 *   });
 */
@Component({
    selector: 'dot-permissions-iframe-dialog',
    standalone: true,
    imports: [DotMessagePipe],
    template: `
        @let src = iframeSrc();

        @if (src) {
            <iframe
                [src]="src"
                class="block w-full border-none"
                [style.min-height]="minHeight()"
                title="Permissions"
                data-testid="permissions-iframe"></iframe>
        } @else {
            <p class="p-3 m-0 text-500" data-testid="permissions-empty">
                {{ 'dot.permissions.iframe.dialog.no-asset' | dm }}
            </p>
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPermissionsIframeDialogComponent {
    readonly #config = inject(DynamicDialogConfig<DotPermissionsIframeDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    readonly iframeSrc = computed<SafeResourceUrl | null>(() => {
        const url = this.#config.data?.url;
        if (!url) return null;
        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });

    readonly minHeight = computed(() => this.#config.data?.minHeight ?? '60vh');
}
