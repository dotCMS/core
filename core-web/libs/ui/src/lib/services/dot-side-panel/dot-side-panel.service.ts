import { Observable, Subject } from 'rxjs';

import {
    DestroyRef,
    Injector,
    Service,
    Signal,
    Type,
    inject,
    isSignal,
    signal
} from '@angular/core';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

/**
 * sessionStorage key remembering the main navigation's collapsed state from BEFORE the first side
 * panel opened, so it can be restored when the last panel closes. `sessionStorage` (not
 * `localStorage`) on purpose: it survives an in-page refresh while a panel is open (e.g. a shared
 * `?editContent=` link, same tab), but is cleared for a genuinely new session — so a key left
 * behind by a tab that closed/crashed before the last panel closed self-clears instead of making the
 * next session wrongly expand a nav the user had chosen to keep collapsed.
 */
const PREV_NAV_COLLAPSED_KEY = 'dot-edit-content-side-panel-prev-nav-collapsed';

function readPrevNavCollapsed(): boolean | null {
    try {
        const raw = sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY);

        return raw === null ? null : raw === 'true';
    } catch {
        return null;
    }
}

function writePrevNavCollapsed(collapsed: boolean): void {
    try {
        sessionStorage.setItem(PREV_NAV_COLLAPSED_KEY, String(collapsed));
    } catch {
        // best-effort: storage failures must not break the panel.
    }
}

function clearPrevNavCollapsed(): void {
    try {
        sessionStorage.removeItem(PREV_NAV_COLLAPSED_KEY);
    } catch {
        // best-effort.
    }
}

/**
 * Collapse the main navigation for a side panel below this viewport width (px).
 *
 * Note this makes collapsing the COMMON case, not an edge case: the panel takes 80% of the
 * viewport, so the strip left beside it only clears the 270px expanded nav by a useful margin on
 * very wide screens. In practice everything below ultra-wide / native 4K collapses — and since
 * `innerWidth` is CSS pixels, a scaled or non-maximized 2560 display lands here too.
 */
const COLLAPSE_MAX_WIDTH = 2560;

/** How long the toast saying a panel's content could not load stays up (ms). */
const LOAD_ERROR_LIFE = 5000;

/**
 * What a side panel's content implements to guard its close: unsaved changes, a save under way.
 * Without it, a close request closes the panel straight away.
 */
export interface DotSidePanelContent {
    /**
     * The author asked to close: the X, Escape, a click on the mask, or browser Back routed by the
     * opener. Close with {@link DotSidePanelRef.close} once it is safe.
     */
    requestClose(): void;
}

/** How to open a side panel. */
export interface DotSidePanelConfig {
    /** The title in the panel's header. A signal keeps it up to date while the panel is open. */
    heading: string | Signal<string>;
    /**
     * The content's inputs, by public name. A signal stays bound for as long as the panel is open;
     * any other value is set once.
     */
    inputs?: Record<string, unknown>;
    /** Handlers for the content's outputs, by public name. */
    outputs?: Record<string, (value: never) => void>;
    /**
     * The opener's injector. The content resolves its dependencies through it, as if it were in the
     * opener's template, and the panel closes when the opener is destroyed.
     */
    injector: Injector;
}

/**
 * One open side panel. The content injects it to close the panel; the opener keeps it to route a
 * close of its own, such as browser Back, through the content's guard.
 */
export class DotSidePanelRef {
    readonly #closed = new Subject<void>();
    readonly #remove: () => void;
    #content: Partial<DotSidePanelContent> | null = null;
    #isClosed = false;

    /** Emits once, when the panel closes, then completes. */
    readonly closed: Observable<void> = this.#closed.asObservable();

    /**
     * Classes for the drawer's content area. No padding by default; content that wants the
     * drawer's padding, or a flex layout, sets its own.
     */
    readonly contentStyleClass = signal('p-0!');

    /** @param remove takes the panel off the screen */
    constructor(remove: () => void) {
        this.#remove = remove;
    }

    /** Whether the panel has closed. */
    get isClosed(): boolean {
        return this.#isClosed;
    }

    /**
     * Records the content, so {@link requestClose} can ask it first. Called by the outlet once the
     * content exists.
     *
     * @param content the content component the panel shows
     */
    attach(content: unknown): void {
        this.#content = content as Partial<DotSidePanelContent>;
    }

    /** Closes the panel now, with no guard. A no-op once closed. */
    close(): void {
        if (this.#isClosed) {
            return;
        }

        this.#isClosed = true;
        this.#remove();
        this.#closed.next();
        this.#closed.complete();
    }

    /**
     * Asks to close: the content's guard decides when the content implements
     * {@link DotSidePanelContent}; otherwise the panel closes straight away.
     */
    requestClose(): void {
        if (typeof this.#content?.requestClose === 'function') {
            this.#content.requestClose();

            return;
        }

        this.close();
    }
}

/**
 * Whether `component` is a promise of the content rather than the content itself. Checked by shape,
 * not `instanceof Promise`: a dynamic `import()` can resolve to another realm's Promise.
 */
function isPending(
    component: Type<unknown> | Promise<Type<unknown>>
): component is Promise<Type<unknown>> {
    return typeof (component as Promise<Type<unknown>>).then === 'function';
}

/** An open panel, as the outlet renders it. */
export interface DotSidePanelEntry {
    readonly id: number;
    readonly ref: DotSidePanelRef;
    readonly heading: Signal<string>;
    /** The content component, or `null` while it loads. */
    readonly component: Signal<Type<unknown> | null>;
    readonly config: DotSidePanelConfig;
}

/**
 * Opens side panels over whatever screen the author is on. Every side panel in the admin opens
 * here and renders in the single `<dot-side-panel-outlet>` at the root of the app, stacked in open
 * order: the last one opened is in front.
 *
 * While any panel is open, the main navigation (logo + menus) is collapsed to make room, on
 * viewports below {@link COLLAPSE_MAX_WIDTH}. It collapses when the first panel opens and is
 * restored when the last one closes, but only if it was expanded to begin with: a nav the author
 * had already collapsed stays collapsed. Panels can stack (the relationship field opens one over
 * another), so the nav stays collapsed until every one has closed. The pre-panel state is kept in
 * sessionStorage, so it survives a refresh with a panel open but not a new session (see
 * {@link PREV_NAV_COLLAPSED_KEY}).
 */
@Service()
export class DotSidePanelService {
    readonly #injector = inject(Injector);
    readonly #panels = signal<readonly DotSidePanelEntry[]>([]);
    #nextId = 0;

    /** The open panels, in open order. */
    readonly panels = this.#panels.asReadonly();

    /**
     * Whether an open panel collapses the main navigation on this viewport. True for most desktop
     * widths; see {@link COLLAPSE_MAX_WIDTH} for why that is intended.
     */
    collapsesNavigation(): boolean {
        return window.innerWidth < COLLAPSE_MAX_WIDTH;
    }

    /**
     * Opens a panel showing `component`. Pass a promise, such as a dynamic `import()`, to keep the
     * content out of the opener's bundle: the panel opens at once and shows a spinner until it
     * loads. If it fails to load, a toast says so and the panel closes.
     *
     * @param component the content, or a promise of it
     * @param config the heading, the content's inputs and outputs, and the opener's injector
     * @returns the open panel
     */
    open(
        component: Type<unknown> | Promise<Type<unknown>>,
        config: DotSidePanelConfig
    ): DotSidePanelRef {
        const loaded = signal<Type<unknown> | null>(isPending(component) ? null : component);
        const ref: DotSidePanelRef = new DotSidePanelRef(() => {
            this.#panels.update((panels) => panels.filter((panel) => panel.ref !== ref));

            if (this.#panels().length === 0) {
                this.#restoreNavigation();
            }
        });

        if (this.#panels().length === 0) {
            this.#collapseNavigation();
        }

        this.#panels.update((panels) => [
            ...panels,
            {
                id: ++this.#nextId,
                ref,
                heading: isSignal(config.heading) ? config.heading : signal(config.heading),
                component: loaded.asReadonly(),
                config
            }
        ]);

        if (isPending(component)) {
            component.then(
                (type) => {
                    if (!ref.isClosed) {
                        loaded.set(type);
                    }
                },
                (error: unknown) => {
                    console.error('Side panel content failed to load', error);
                    this.#reportLoadError();
                    ref.close();
                }
            );
        }

        // The panel lives no longer than whoever opened it.
        const stopWatchingOpener = config.injector
            .get(DestroyRef, null)
            ?.onDestroy(() => ref.close());
        ref.closed.subscribe({ complete: () => stopWatchingOpener?.() });

        return ref;
    }

    /**
     * Tells the author a panel's content could not load (after a deploy, say, or while offline).
     * Through the admin's shared toast rather than PrimeNG's `MessageService`, which not every
     * screen provides. Resolved here, as the store is, since most panels load fine.
     */
    #reportLoadError(): void {
        this.#injector.get(DotMessageDisplayService, null)?.push({
            life: LOAD_ERROR_LIFE,
            message: this.#injector
                .get(DotMessageService)
                .get('edit.content.side-panel.load-error'),
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }

    /**
     * Collapses an expanded main navigation as the first panel opens, remembering how it was. The
     * store is resolved here rather than with the service: openers inject the service up front, and
     * most never open a panel.
     */
    #collapseNavigation(): void {
        if (!this.collapsesNavigation()) {
            return;
        }

        const navigation = this.#injector.get(GlobalStore);

        // Write-if-absent: a refresh while a panel is open must not overwrite the ORIGINAL
        // pre-panel state with the (already-collapsed) current one.
        if (readPrevNavCollapsed() === null) {
            writePrevNavCollapsed(navigation.isNavigationCollapsed());
        }

        if (!navigation.isNavigationCollapsed()) {
            navigation.collapseNavigation();
        }
    }

    /** Restores the main navigation as the last panel closes, if it was expanded before. */
    #restoreNavigation(): void {
        if (readPrevNavCollapsed() === false) {
            this.#injector.get(GlobalStore).expandNavigation();
        }

        clearPrevNavCollapsed();
    }
}
