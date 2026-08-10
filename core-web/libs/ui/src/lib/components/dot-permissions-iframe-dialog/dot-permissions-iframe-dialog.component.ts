import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotPermissionsIframeComponent } from './dot-permissions-iframe.component';

export interface DotPermissionsIframeDialogData {
    url: string;
    minHeight?: string;
}

/**
 * Modal wrapper around {@link DotPermissionsIframeComponent}. Reads its
 * URL from the {@link DynamicDialogConfig} so it can be dropped straight
 * into `DialogService.open(...)` calls. Kept as a thin adapter — all URL
 * validation and DOM rendering lives in the presentational component so
 * embedding the permissions iframe inline (e.g. inside a `<p-tabpanel>`)
 * doesn't require the dialog machinery.
 *
 * Usage:
 *   dialogService.open(DotPermissionsIframeDialogComponent, {
 *     header: 'Permissions',
 *     data: { url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=...' }
 *   });
 */
@Component({
    selector: 'dot-permissions-iframe-dialog',
    imports: [DotPermissionsIframeComponent],
    template: `
        <dot-permissions-iframe [url]="$url()" [minHeight]="$minHeight()" />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPermissionsIframeDialogComponent {
    readonly #config = inject(DynamicDialogConfig<DotPermissionsIframeDialogData>);

    readonly $url = computed(() => this.#config.data?.url ?? '');
    readonly $minHeight = computed(() => this.#config.data?.minHeight ?? '60vh');
}
