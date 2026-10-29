import {
    DestroyRef,
    Injector,
    Signal,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';

import { DotSidePanelRef } from '@dotcms/ui';

import { DotEditContentOpenerService } from './dot-edit-content-opener.service';

import { DotEditContentPanelHandlers } from '../models/dot-edit-content-panel-events.model';
import { DotEditContentRequest } from '../models/dot-edit-content-request.model';

/** The events a surface handles for the panel {@link injectEditContentPanel} keeps open. */
export interface DotEditContentPanelBinding extends DotEditContentPanelHandlers {
    /**
     * The panel closed itself: the X, Escape, a mask click, a delete, or browser Back routed by the
     * surface. Clear the request here. Not called when the panel closes because the request was
     * cleared or the opener was destroyed.
     */
    closed?: () => void;
}

/** The panel {@link injectEditContentPanel} keeps open, as the surface sees it. */
export interface DotEditContentPanelHandle {
    /** Whether the panel is open. */
    readonly isOpen: Signal<boolean>;
    /**
     * Asks the open panel to close through its editor's unsaved-changes guard, as its own close
     * button does. For closes the surface handles itself, such as browser Back. A no-op while
     * closed.
     */
    requestClose(): void;
}

/**
 * Keeps an edit-content side panel open for as long as `request` holds a value, through
 * {@link DotEditContentOpenerService}: opens it when the request is set, keeps its heading and
 * request in step while it stays set, and closes it, with no guard, when the request is cleared.
 * Same shape as `injectSidePanel`, for content.
 *
 * A request that changes while the panel is open does not reopen it, so a surface that wants a
 * fresh editor for another content clears the request first.
 *
 * Call it from a constructor or a field initializer: it needs an injection context, and the panel
 * closes when the surface is destroyed.
 *
 * @param request What the panel shows, or nothing while it is closed.
 * @param binding The events the surface handles.
 * @returns The panel, so the surface can route a close of its own through the editor's guard.
 */
export function injectEditContentPanel(
    request: Signal<DotEditContentRequest | null | undefined>,
    binding: DotEditContentPanelBinding
): DotEditContentPanelHandle {
    const opener = inject(DotEditContentOpenerService);
    const injector = inject(Injector);

    // The request the open panel shows. Kept apart from `request` so the heading and the panel's
    // request keep their last value while the panel closes, instead of reading a cleared one.
    const shown = signal<DotEditContentRequest | null>(null);
    const current = computed(() => shown() as DotEditContentRequest);
    const panel = signal<DotSidePanelRef | null>(null);

    // The surface is going away: forget the panel before closing it, so the close is not reported to
    // a screen that no longer exists. Registered before any panel opens, so it runs before the
    // side-panel service closes the panel for the same reason.
    inject(DestroyRef).onDestroy(() => {
        const closing = panel();
        panel.set(null);
        closing?.close();
    });

    const open = (): void => {
        const { closed, ...handlers } = binding;

        const ref = opener.open(current, {
            ...handlers,
            injector,
            closed: () => {
                // Only a panel this surface still shows reports its close. Nothing is showing yet
                // when a refused request closes during `open`, so `ref` is never read before then.
                const showing = panel();

                if (showing && showing === ref) {
                    panel.set(null);
                    closed?.();
                }
            }
        });

        // A request the opener refused opened nothing: report it closed, so the surface clears it.
        if (ref.isClosed) {
            closed?.();

            return;
        }

        panel.set(ref);
    };

    effect(() => {
        const value = request();

        untracked(() => {
            if (value !== null && value !== undefined) {
                shown.set(value);

                if (!panel()) {
                    open();
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
