import { Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { isSameOriginRelativeUrl } from '@dotcms/utils';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotJspIframeDialogData {
    /** Same-origin relative path to the JSP, query string included. Built by the caller. */
    url: string;
    /** i18n key for the iframe's accessible title. */
    titleKey: string;
    /** i18n key shown instead of the frame when `url` is missing or not same-origin. */
    emptyKey: string;
    /** Prefix for this dialog's two `data-testid`s, so each caller's tests select their own. */
    testIdPrefix: string;
    minHeight?: string;
}

/**
 * Hosts a legacy JSP screen in a dialog.
 *
 * Every screen dotCMS has not yet rebuilt in Angular is reached this way — an iframe pointed at a
 * `/html/portlet/...` path — so what varies between them is strings, not behaviour. This used to be
 * two components (permissions and push history) that `diff` showed to be identical apart from an
 * interface name, a title, a test id and an empty-state key; the second was written because the
 * first was named for one caller and so did not look reusable. Hence the neutral name: the next
 * legacy screen should add a call site, not a third copy.
 *
 * **Only same-origin relative paths are accepted.** `isSameOriginRelativeUrl` resolves the URL and
 * compares origins rather than pattern-matching it, because a leading-slash check passes strings
 * that browsers resolve off-origin (`/\evil.com`, and tab/newline variants).
 *
 * Usage:
 *   dialogService.open(DotJspIframeDialogComponent, {
 *     header: this.#dotMessageService.get('publisher_push_history'),
 *     data: {
 *       url: '/html/portlet/ext/folders/push_history.jsp?folderIdentifier=...&popup=true',
 *       titleKey: 'publisher_push_history',
 *       emptyKey: 'dot.push-history.iframe.dialog.no-asset',
 *       testIdPrefix: 'push-history'
 *     }
 *   });
 */
@Component({
    selector: 'dot-jsp-iframe-dialog',
    imports: [DotMessagePipe],
    templateUrl: './dot-jsp-iframe-dialog.component.html'
})
export class DotJspIframeDialogComponent {
    readonly #config = inject(DynamicDialogConfig<DotJspIframeDialogData>);
    readonly #sanitizer = inject(DomSanitizer);

    readonly $iframeSrc = computed<SafeResourceUrl | null>(() => {
        const url = this.#config.data?.url;
        if (!isSameOriginRelativeUrl(url)) return null;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });

    readonly $minHeight = computed(() => this.#config.data?.minHeight ?? '60vh');

    readonly $titleKey = computed(() => this.#config.data?.titleKey ?? '');

    readonly $emptyKey = computed(() => this.#config.data?.emptyKey ?? '');

    /**
     * Falls back to `jsp` rather than to an empty string: a bare `-iframe` test id would collide
     * across callers, which is the opposite of what the prefix exists for.
     */
    readonly $testIdPrefix = computed(() => this.#config.data?.testIdPrefix ?? 'jsp');
}
