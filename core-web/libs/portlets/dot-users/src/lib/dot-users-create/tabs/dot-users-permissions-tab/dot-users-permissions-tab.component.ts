import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessagePipe } from '@dotcms/ui';
import { isSameOriginRelativeUrl } from '@dotcms/utils';

/**
 * Thin wrapper JSP that resolves the user's implicit role and delegates to
 * the shared `view_role_permissions_js_inc.jsp` fragment. Named after the
 * `*_wrapper.jsp` convention in this tree — a standalone page whose only
 * job is to host a legacy fragment inside an iframe, same as roleadmin's
 * `view_role_permissions_wrapper.jsp`.
 */
const PERMISSIONS_JSP = '/html/portlet/ext/useradmin/view_users_permissions_wrapper.jsp';

/**
 * Element the wrapper JSP emits last on the granted path. Its presence
 * is the only reliable "the permissions UI rendered" signal — see
 * `onLoad`. Keep in sync with the JSP.
 */
const READY_MARKER_ID = 'dot-permissions-ready';

/**
 * How long to wait for the iframe's `load` event before giving up.
 * An iframe fires no `error` event for a hung request, a connection the
 * proxy drops mid-flight, or a navigation blocked by frame-ancestors —
 * without this watchdog those cases sit on the skeleton forever.
 */
const LOAD_TIMEOUT_MS = 20_000;

/**
 * Permissions tab. Renders the legacy Dojo role-permissions UI inside an
 * iframe until the native Angular implementation against
 * GET/PUT /api/v1/permissions/user/{userId} lands.
 *
 * `popup=true` is mandatory — without it the legacy `top_inc.jsp` renders
 * `<body style="visibility:hidden">` and the iframe shows up blank.
 *
 * In create mode the userId does not exist yet, so we render an empty
 * state prompting the user to save first.
 *
 * Kept local to this portlet on purpose: the roles portlet embeds its own
 * permissions iframe the same way (`dot-role-permissions-iframe`), and
 * neither is generic enough to earn a slot in `@dotcms/ui`. The one piece
 * that is genuinely shared — the same-origin guard — already lives in
 * `@dotcms/utils` and is reused here.
 */
@Component({
    selector: 'dot-users-permissions-tab',
    imports: [ButtonModule, DotMessagePipe, SkeletonModule],
    templateUrl: './dot-users-permissions-tab.component.html',
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotUsersPermissionsTabComponent {
    readonly #sanitizer = inject(DomSanitizer);

    readonly userId = input<string | null>(null);

    /**
     * Bumped by `retry()`. Riding along in the query string is what makes
     * the retry declarative: a new URL means a new `$iframeSrc`, which the
     * effect below already treats as "start over" — reset to `loading` and
     * re-arm the watchdog. The JSP ignores unknown params.
     */
    readonly #$retryCount = signal(0);

    readonly $permissionsUrl = computed<string>(() => {
        const id = this.userId();
        if (!id) {
            return '';
        }
        const params = new URLSearchParams({ userId: id, popup: 'true' });
        const retry = this.#$retryCount();
        if (retry > 0) {
            params.set('_retry', String(retry));
        }

        return `${PERMISSIONS_JSP}?${params.toString()}`;
    });

    /**
     * Iframe load lifecycle:
     * - `loading` — src set, no load event yet. Skeleton shown, iframe
     *   is transparent so paint flashes don't leak through.
     * - `loaded` — the wrapper JSP rendered the permissions UI.
     * - `unavailable` — it did not: the id didn't resolve, or the viewer
     *   lacks READ on the target user. Same-origin so `contentDocument`
     *   is readable.
     * - `timeout` — no `load` event within `LOAD_TIMEOUT_MS`. Distinct
     *   from `unavailable` because the cause is transport, not access,
     *   and retrying is the right advice.
     */
    protected readonly $state = signal<'loading' | 'loaded' | 'unavailable' | 'timeout'>('loading');

    protected readonly $iframeSrc = computed<SafeResourceUrl | null>(() => {
        // Last sanitizer boundary before `bypassSecurityTrustResourceUrl`.
        // The path is a hard-coded constant and the only interpolated
        // value goes through `URLSearchParams`, so this can't fail today
        // — it's here so a future edit to `PERMISSIONS_JSP` can't turn
        // the tab into an open frame-redirect. Same predicate the JSP
        // iframe dialog in `@dotcms/ui` uses.
        const url = this.$permissionsUrl();
        if (!isSameOriginRelativeUrl(url)) return null;

        return this.#sanitizer.bypassSecurityTrustResourceUrl(url);
    });

    constructor() {
        // Reset the load state whenever the derived src changes so
        // switching users shows the skeleton again, and arm the watchdog
        // for that navigation. Kept out of the computed above because
        // Angular forbids signal writes there.
        effect((onCleanup) => {
            const src = this.$iframeSrc();
            if (!src) {
                return;
            }

            untracked(() => this.$state.set('loading'));

            const timeoutId = setTimeout(() => {
                // Only the still-pending navigation may time out; a
                // `load` that already resolved the state wins.
                if (this.$state() === 'loading') {
                    this.$state.set('timeout');
                }
            }, LOAD_TIMEOUT_MS);

            // Runs on the next src change and on destroy.
            onCleanup(() => clearTimeout(timeoutId));
        });
    }

    /** Re-navigates the iframe after a timeout. */
    protected retry(): void {
        this.#$retryCount.update((count) => count + 1);
    }

    protected onLoad(event: Event): void {
        const iframe = event.target as HTMLIFrameElement | null;
        // Same-origin guard above means `contentDocument` is
        // reachable; the try/catch is defence-in-depth in case a
        // sandboxed iframe or a redirect ever changes the story.
        try {
            // Positive marker, not an emptiness check: `top_inc.jsp` and
            // `messages_inc.jsp` run on the failure path too and leave
            // `<script>` elements in the body, so a failed render is
            // never a blank body. The wrapper JSP emits this element as
            // its last output on the granted path only.
            const rendered = iframe?.contentDocument?.getElementById(READY_MARKER_ID);
            this.$state.set(rendered ? 'loaded' : 'unavailable');
        } catch {
            // Can't tell — assume loaded so we don't hide a
            // rendered but cross-origin child.
            this.$state.set('loaded');
        }
    }
}
