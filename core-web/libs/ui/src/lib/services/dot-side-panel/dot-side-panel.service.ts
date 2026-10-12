import { Observable, Subject } from 'rxjs';

import { DestroyRef, Injectable, Injector, Signal, Type, isSignal, signal } from '@angular/core';

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
 */
@Injectable({ providedIn: 'root' })
export class DotSidePanelService {
    readonly #panels = signal<readonly DotSidePanelEntry[]>([]);
    #nextId = 0;

    /** The open panels, in open order. */
    readonly panels = this.#panels.asReadonly();

    /**
     * Opens a panel showing `component`. Pass a promise, such as a dynamic `import()`, to keep the
     * content out of the opener's bundle: the panel opens at once and shows a spinner until it
     * loads.
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
        const ref: DotSidePanelRef = new DotSidePanelRef(() =>
            this.#panels.update((panels) => panels.filter((panel) => panel.ref !== ref))
        );

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
}
