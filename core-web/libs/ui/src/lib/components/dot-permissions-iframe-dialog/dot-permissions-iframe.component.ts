import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * Presentational iframe wrapper around a legacy permissions JSP. Callers
 * are responsible for building the URL. Only same-origin relative paths
 * are accepted so a caller cannot navigate the parent frame to an
 * arbitrary origin.
 *
 * Usage as an inline tab:
 *   <dot-permissions-iframe
 *     [url]="/html/portlet/ext/useradmin/permissions.jsp?userId=…&popup=true"
 *     minHeight="34rem" />
 *
 * The dialog variant `DotPermissionsIframeDialogComponent` is a thin
 * adapter over this component; see that file for the modal wiring.
 */
@Component({
    selector: 'dot-permissions-iframe',
    imports: [DotMessagePipe],
    template: `
        @let src = $iframeSrc();

        @if (src) {
            <iframe
                [src]="src"
                class="block w-full border-none"
                [style.height]="height()"
                [style.min-height]="height() ? null : minHeight()"
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
export class DotPermissionsIframeComponent {
    readonly #sanitizer = inject(DomSanitizer);

    readonly url = input.required<string>();
    readonly minHeight = input<string>('60vh');
    /**
     * When provided, applied as a fixed `height` and `min-height` is
     * ignored. Use this when the iframe lives inside a bounded container
     * (e.g. a tab panel) so its own scrollbar handles overflow instead
     * of the outer container's.
     */
    readonly height = input<string | null>(null);

    readonly $iframeSrc = computed<SafeResourceUrl | null>(() => {
        const url = this.url();
        if (!url) return null;
        // Only allow same-origin relative paths; reject absolute URLs,
        // protocol-relative URLs, and dangerous schemes (javascript:,
        // data:, http:, etc.)
        if (!url.startsWith('/') || url.startsWith('//')) return null;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });
}
