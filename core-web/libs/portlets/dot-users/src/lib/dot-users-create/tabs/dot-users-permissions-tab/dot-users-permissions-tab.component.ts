import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

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
    imports: [DotMessagePipe, SkeletonModule],
    templateUrl: './dot-users-permissions-tab.component.html',
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotUsersPermissionsTabComponent {
    readonly #sanitizer = inject(DomSanitizer);

    readonly userId = input<string | null>(null);

    readonly $permissionsUrl = computed<string>(() => {
        const id = this.userId();
        if (!id) {
            return '';
        }
        const params = new URLSearchParams({ userId: id, popup: 'true' });

        return `${PERMISSIONS_JSP}?${params.toString()}`;
    });

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
        // switching users shows the skeleton again. Kept out of the
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
