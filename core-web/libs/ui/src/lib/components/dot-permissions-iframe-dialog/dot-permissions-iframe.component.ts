import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { SkeletonModule } from 'primeng/skeleton';

import { isSameOriginRelativeUrl } from '@dotcms/utils';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * Presentational iframe wrapper around a legacy permissions JSP. Callers
 * are responsible for building the URL. Only same-origin relative paths
 * are accepted so a caller cannot navigate the parent frame to an
 * arbitrary origin — same-origin resolution delegates to the shared
 * `isSameOriginRelativeUrl` predicate (see `@dotcms/utils`), which the
 * dialog-flavoured `dot-jsp-iframe-dialog` also uses.
 *
 * Usage as an inline tab:
 *   <dot-permissions-iframe
 *     [url]="/html/portlet/ext/useradmin/permissions.jsp?userId=…&popup=true"
 *     minHeight="34rem" />
 */
@Component({
    selector: 'dot-permissions-iframe',
    imports: [DotMessagePipe, SkeletonModule],
    template: `
        @let src = $iframeSrc();

        @if (src) {
            <div class="relative flex min-h-0 w-full flex-1 flex-col">
                @if ($state() === 'loading') {
                    <div
                        class="absolute inset-0 z-10 flex flex-col gap-3 p-4"
                        data-testid="permissions-loading">
                        <p-skeleton height="2rem" width="60%" />
                        <p-skeleton height="1rem" width="90%" />
                        <p-skeleton height="1rem" width="85%" />
                        <p-skeleton height="1rem" width="80%" />
                    </div>
                }
                @if ($state() === 'empty') {
                    <p class="text-color-secondary m-0 p-3" data-testid="permissions-empty-body">
                        {{ 'dot.permissions.iframe.dialog.empty-body' | dm }}
                    </p>
                }
                <iframe
                    [src]="src"
                    class="w-full flex-1 border-none"
                    [style.height]="height()"
                    [style.min-height]="height() ? null : minHeight()"
                    [class.opacity-0]="$state() !== 'loaded'"
                    [title]="iframeTitle()"
                    (load)="onLoad($event)"
                    data-testid="permissions-iframe"></iframe>
            </div>
        } @else {
            <p class="text-500 m-0 p-3" data-testid="permissions-empty">
                {{ 'dot.permissions.iframe.dialog.no-asset' | dm }}
            </p>
        }
    `
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

    /**
     * Screen-reader `title` for the iframe. Callers should pass an
     * already-translated string so this shared component doesn't tie
     * itself to a specific message key. Falls back to `Permissions`
     * only to keep the un-i18n'd caller from ending up with an empty
     * label.
     */
    readonly iframeTitle = input<string>('Permissions');

    /**
     * Iframe load lifecycle:
     * - `loading` — src set, no load event yet. Skeleton shown, iframe
     *   is transparent so paint flashes don't leak through.
     * - `loaded` — the JSP responded with a non-empty body.
     * - `empty` — the JSP responded with a blank body (typically means
     *   the viewer lacks portlet access or the id didn't resolve — the
     *   legacy `top_inc.jsp` renders `<body>` with no content in that
     *   case). Same-origin so `contentDocument` is readable.
     */
    protected readonly $state = signal<'loading' | 'loaded' | 'empty'>('loading');

    readonly $iframeSrc = computed<SafeResourceUrl | null>(() => {
        // Last sanitizer boundary before `bypassSecurityTrustResourceUrl`
        // — reject absolute URLs, protocol-relative URLs, and dangerous
        // schemes. Delegated to the shared predicate so backslash /
        // tab / newline variants normalized by the URL parser
        // (`/\evil.com` → `//evil.com` after browser normalization)
        // are caught by resolution-and-compare instead of prefix
        // heuristics.
        const url = this.url();
        if (!isSameOriginRelativeUrl(url)) return null;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });

    constructor() {
        // Reset the load state whenever the derived src changes so a
        // navigation shows the skeleton again. Kept out of the
        // computed above because Angular forbids signal writes there.
        effect(() => {
            const src = this.$iframeSrc();
            if (src) {
                untracked(() => this.$state.set('loading'));
            }
        });
    }

    protected onLoad(event: Event): void {
        const iframe = event.target as HTMLIFrameElement | null;
        // Same-origin guard above means `contentDocument` is
        // reachable; the try/catch is defence-in-depth in case a
        // sandboxed iframe or a redirect ever changes the story.
        try {
            const body = iframe?.contentDocument?.body;
            const html = body?.innerHTML?.trim() ?? '';
            this.$state.set(html.length === 0 ? 'empty' : 'loaded');
        } catch {
            // Can't tell — assume loaded so we don't hide a
            // rendered but cross-origin child.
            this.$state.set('loaded');
        }
    }
}
