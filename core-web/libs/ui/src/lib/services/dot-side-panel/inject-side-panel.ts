import {
    DestroyRef,
    Injector,
    Signal,
    Type,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';

import { DotSidePanelRef, DotSidePanelService } from './dot-side-panel.service';

/** What {@link injectSidePanel} opens for a request, and how it reports back. */
export interface DotSidePanelBinding<R> {
    /**
     * The content to show, called on each open. Return a dynamic `import()` to keep the content out
     * of the opener's bundle.
     */
    component: () => Type<unknown> | Promise<Type<unknown>>;
    /** The title in the panel's header, for the current request. */
    heading: (request: R) => string;
    /**
     * The content's inputs, by public name, for the current request. Their values follow the
     * request, but the names are read once, when the panel opens: return the same names for every
     * request, since one added later is never bound.
     */
    inputs?: (request: R) => Record<string, unknown>;
    /** Handlers for the content's outputs, by public name. */
    outputs?: Record<string, (value: never) => void>;
    /**
     * The panel closed itself: the author closed it, or its content did. Clear the request here. Not
     * called when the panel closes because the request was cleared or the opener was destroyed.
     */
    closed?: () => void;
}

/** The panel {@link injectSidePanel} keeps open, as the opener sees it. */
export interface DotSidePanelHandle {
    /** Whether the panel is open. */
    readonly isOpen: Signal<boolean>;
    /**
     * Asks the open panel to close through its content's guard, as its own close button does. For
     * closes the opener handles itself, such as browser Back. A no-op while closed.
     */
    requestClose(): void;
}

/**
 * Keeps a side panel open for as long as `request` holds a value: opens it when the request is
 * set, keeps its heading and inputs in step with the request while it stays set, and closes it,
 * with no guard, when the request is cleared. The panel renders in the app's root outlet, not in
 * the opener's template.
 *
 * Call it from a constructor or a field initializer: it needs an injection context, and the panel
 * closes when the opener is destroyed.
 *
 * @param request what the panel shows, or nothing while it is closed
 * @param binding the content, its heading, inputs and outputs, and what to do when it closes
 * @returns the panel, so the opener can route a close of its own through the content's guard
 */
export function injectSidePanel<R>(
    request: Signal<R | null | undefined>,
    binding: DotSidePanelBinding<R>
): DotSidePanelHandle {
    const sidePanels = inject(DotSidePanelService);
    const injector = inject(Injector);

    // The request the open panel shows. Kept apart from `request` so the heading and inputs keep
    // their last value while the panel closes, instead of reading a cleared request.
    const shown = signal<R | null>(null);
    const panel = signal<DotSidePanelRef | null>(null);

    // The opener is going away: forget the panel before closing it, so the close is not reported to
    // a screen that no longer exists. Registered before any panel opens, so it runs before the
    // service closes the panel for the same reason.
    inject(DestroyRef).onDestroy(() => {
        const closing = panel();
        panel.set(null);
        closing?.close();
    });

    const open = (value: R): DotSidePanelRef => {
        const current = () => shown() as R;
        const inputNames = Object.keys(binding.inputs?.(value) ?? {});
        const ref = sidePanels.open(binding.component(), {
            heading: computed(() => binding.heading(current())),
            inputs: Object.fromEntries(
                inputNames.map((name) => [name, computed(() => binding.inputs?.(current())[name])])
            ),
            outputs: binding.outputs,
            injector
        });

        ref.closed.subscribe(() => {
            if (panel() === ref) {
                panel.set(null);
                binding.closed?.();
            }
        });

        return ref;
    };

    effect(() => {
        const value = request();

        untracked(() => {
            if (value !== null && value !== undefined) {
                shown.set(value);

                if (!panel()) {
                    panel.set(open(value));
                }

                return;
            }

            const closing = panel();
            if (closing) {
                panel.set(null);
                closing.close();
            }
        });
    });

    return {
        isOpen: computed(() => panel() !== null),
        requestClose: () => panel()?.requestClose()
    };
}
