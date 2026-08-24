import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { isSameOriginRelativeUrl } from '@dotcms/utils';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotPushHistoryIframeDialogData {
    url: string;
    minHeight?: string;
}

/**
 * Dialog component that displays an asset's push publish history in an iframe.
 * Callers are responsible for building the URL before opening the dialog.
 * Only same-origin relative paths (starting with `/`) are accepted.
 *
 * Usage:
 *   dialogService.open(DotPushHistoryIframeDialogComponent, {
 *     header: 'Push History',
 *     data: { url: '/html/portlet/ext/folders/push_history.jsp?folderIdentifier=...&popup=true' }
 *   });
 */
@Component({
    selector: 'dot-push-history-iframe-dialog',
    imports: [DotMessagePipe],
    templateUrl: './dot-push-history-iframe-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPushHistoryIframeDialogComponent {
    readonly #config = inject(DynamicDialogConfig<DotPushHistoryIframeDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    readonly $iframeSrc = computed<SafeResourceUrl | null>(() => {
        const url = this.#config.data?.url;
        if (!isSameOriginRelativeUrl(url)) return null;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });

    readonly $minHeight = computed(() => this.#config.data?.minHeight ?? '60vh');
}
