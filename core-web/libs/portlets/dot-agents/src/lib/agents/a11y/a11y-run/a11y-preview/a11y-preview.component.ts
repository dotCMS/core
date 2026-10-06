import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    isDevMode,
    viewChild
} from '@angular/core';

import { A11yGroup } from '@dotcms/portlets/dot-ema/ui';

import { DotA11yPreviewFrameComponent } from './a11y-preview-frame/a11y-preview-frame.component';

import { StudioPageRow } from '../../models/accessibility-studio.models';
import { A11yMarkerService } from '../../services/a11y-marker.service';

/**
 * The run screen's right column: the same page rendered twice, side by side, so
 * the fix reads at a glance —
 *   PREVIEW — the working render (carries the agent's fixes, the "after")
 *   LIVE    — the published render (what visitors see today, pre-fix)
 *
 * Owns everything that has to reach INTO those documents: the violation marker
 * overlay and the scroll mirroring. Both need same-origin frames (see
 * {@link previewPathPrefix}), and both need the two frames together, which is why
 * they live here rather than in {@link DotA11yPreviewFrameComponent}.
 */
@Component({
    selector: 'dot-a11y-preview',
    imports: [DotA11yPreviewFrameComponent],
    templateUrl: './a11y-preview.component.html',
    // The marker service is stateful per rendered pair (it tracks the layers it
    // injected), so it's provided HERE — one instance per preview pane.
    providers: [A11yMarkerService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex min-h-0 flex-col' }
})
export class DotA11yPreviewComponent {
    /** The page to render in both frames. Null renders neither. */
    readonly page = input<StudioPageRow | null>(null);

    /**
     * Cache-buster for the PREVIEW url. The store bumps it whenever the agent applies
     * fixes (each mid-fix re-scan + the final report) so the working render reloads
     * and the page updates visually. LIVE never changes mid-run, so it takes none.
     */
    readonly previewRevision = input(0);

    /** Violations from the PRIMARY (working) scan — drawn over the preview frame. */
    readonly previewGroups = input<A11yGroup[]>([]);

    /** Violations from the COMPARISON (live) scan — drawn over the live frame. */
    readonly liveGroups = input<A11yGroup[]>([]);

    /**
     * Whether to draw the overlays at all. Each frame draws its OWN scan's findings,
     * so the only shared gate is: a scan pass has run. Empty groups (the live scan
     * hasn't landed yet, or a frame came back clean) simply draw no markers.
     */
    readonly showMarkers = input(false);

    readonly #markerService = inject(A11yMarkerService);

    // NOTE: `private`, not `#`. Angular rejects an ES-private member for a signal query
    // outright — "Cannot use 'viewChild' on a class member that is declared as ES private"
    // — because the compiler has to write to the field from generated code.
    private readonly $previewFrame = viewChild<DotA11yPreviewFrameComponent>('previewFrame');
    private readonly $liveFrame = viewChild<DotA11yPreviewFrameComponent>('liveFrame');

    constructor() {
        // Redraw both frames' marker layers whenever their scans (or the gate)
        // change. Each frame gets its OWN scan's findings: the preview frame from
        // the primary/working scan, the live frame from the comparison scan. They
        // are separate scans, so a fix that isn't published yet clears the preview
        // markers while the live markers (still-published violations) remain.
        effect(() => {
            const show = this.showMarkers();
            const previewGroups = this.previewGroups();
            const liveGroups = this.liveGroups();
            this.#markerService.render(this.$previewFrame()?.element(), show ? previewGroups : []);
            this.#markerService.render(this.$liveFrame()?.element(), show ? liveGroups : []);
        });
    }

    /**
     * Re-entrancy guard for scroll mirroring: setting frame B's scroll fires B's
     * own `scroll` event, which would mirror straight back to A — an infinite
     * bounce. While we're programmatically scrolling the target, ignore its echo.
     */
    #syncingScroll = false;

    /**
     * LIVE iframe finished (re)loading — (re)draw its markers from the LIVE
     * (comparison) scan + (re)wire scroll sync. A load replaces the document, so
     * the effect-drawn layer is gone and must be redrawn here.
     */
    onLiveLoad(): void {
        this.#markerService.render(
            this.$liveFrame()?.element(),
            this.showMarkers() ? this.liveGroups() : []
        );
        this.#wireScrollSync(this.$liveFrame(), this.$previewFrame());
    }

    /**
     * PREVIEW iframe finished (re)loading — (re)draw its markers from the primary
     * (working) scan + (re)wire scroll sync.
     */
    onPreviewLoad(): void {
        this.#markerService.render(
            this.$previewFrame()?.element(),
            this.showMarkers() ? this.previewGroups() : []
        );
        this.#wireScrollSync(this.$previewFrame(), this.$liveFrame());
    }

    /**
     * Mirror `source`'s scroll onto `target` so the two side-by-side renders stay
     * aligned — makes the before/after diff scannable without scrolling each pane
     * separately. Both frames are same-origin (the `/dot-page` proxy / BE origin),
     * so we can read/write `contentWindow.scroll*` directly; cross-origin access
     * throws and we no-op.
     *
     * Wired on every `load`: a reload/navigation replaces the frame's window, which
     * drops the old listener for free, so we just attach a fresh one each time.
     */
    #wireScrollSync(
        source: DotA11yPreviewFrameComponent | undefined,
        target: DotA11yPreviewFrameComponent | undefined
    ): void {
        const srcWin = this.#frameWindow(source);
        if (!srcWin) {
            return;
        }
        srcWin.addEventListener(
            'scroll',
            () => {
                if (this.#syncingScroll) {
                    return;
                }
                const tgtWin = this.#frameWindow(target);
                if (!tgtWin) {
                    return;
                }
                this.#syncingScroll = true;
                tgtWin.scrollTo(srcWin.scrollX, srcWin.scrollY);
                // Release after the target's echoed scroll event has fired.
                requestAnimationFrame(() => (this.#syncingScroll = false));
            },
            { passive: true }
        );
    }

    /** The iframe's window; null when cross-origin or not yet loaded. */
    #frameWindow(frame: DotA11yPreviewFrameComponent | undefined): Window | null {
        try {
            return frame?.element()?.contentWindow ?? null;
        } catch {
            return null;
        }
    }

    /**
     * Same-origin prefix for the preview iframe URLs.
     *
     * In DEV the Angular dev server can't render dotCMS pages, so the iframes must
     * hit the backend. The dev proxy maps the `/dot-page` sentinel → the BE page
     * renderer (see apps/dotcms-ui/proxy-dev.conf.mjs). In PROD the portlet is
     * served from the dotCMS origin, so the page lives at its own path with NO
     * prefix — `/dot-page` would 404 there. `isDevMode()` is build-time accurate
     * (true under `ng serve`, false in a production build) and needs no app-env
     * import, so the dev-only prefix never leaks to production.
     *
     * NOTE: this pairs with the `/dot-page` rule in proxy-dev.conf.mjs — the two
     * must change together, or the preview frames 404 in local dev.
     *
     * Same-origin is a hard requirement, not a convenience: the marker overlay and
     * the scroll sync both reach into each frame's `contentWindow`
     * ({@link frameWindow}), which the browser forbids cross-origin. A cross-origin
     * frame would still render the page but silently draw no violation markers.
     *
     * NEEDS A BACKEND FIX: the proper solution is a first-class, same-origin dotCMS
     * endpoint that renders a page for inspection (a supported resource under
     * `/api`), so this component — and any future agent that has to inspect a
     * rendered page — can frame it directly with no origin games and no dev-server
     * rewrite. No such endpoint exists today; that gap is why the sentinel + proxy
     * pair exists. Delete both once it lands.
     */
    readonly #previewPathPrefix = isDevMode() ? '/dot-page' : '';

    /**
     * The page rendered in the given mode. `host_id` disambiguates which site's
     * copy renders. Shared by the two side-by-side frames. An optional
     * cache-busting `rev` forces the iframe to reload when the working render
     * changes (see {@link previewRevision}).
     */
    #urlFor(mode: 'PREVIEW_MODE' | 'LIVE', rev = 0): string {
        const page = this.page();
        if (!page) {
            return '';
        }
        const path = page.path.startsWith('/') ? page.path : `/${page.path}`;
        const bust = rev > 0 ? `&rev=${rev}` : '';
        return `${this.#previewPathPrefix}${path}?host_id=${page.hostId}&language_id=${page.languageId}&mode=${mode}${bust}`;
    }

    protected readonly $liveUrl = computed(() => this.#urlFor('LIVE'));
    protected readonly $previewUrl = computed(() =>
        this.#urlFor('PREVIEW_MODE', this.previewRevision())
    );

    /** What both address bars show — host + path of the page being compared. */
    protected readonly $address = computed(() => {
        const page = this.page();
        return page ? `${page.hostName}${page.path}` : '';
    });
}
