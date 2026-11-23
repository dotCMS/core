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

import { DotContentEditor } from '@dotcms/data-access';
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

    /**
     * Shows the legacy editor whatever the content type says; see `DotEditContentOpenOptions`.
     * Read each time the panel opens, so a getter can decide per open. A getter that reads a signal
     * is tracked: when its answer changes while the panel is open, the panel reopens in that editor.
     * Remove with the legacy editor.
     */
    editor?: 'legacy';

    /** The editor the panel shows, once known; see `DotEditContentOpenOptions`. */
    editorResolved?: (editor: DotContentEditor) => void;
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
 * A request that changes while the panel is open keeps the panel when it still names the same
 * content (its `mode`, `inode` and `contentType`) in the same editor, as a new title does. One that
 * names other content, or a binding whose `editor` changes, closes the panel, with no guard and no
 * `closed`, and opens a fresh one in the same pass.
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
    // What the open panel was opened for, so a request naming other content reopens it.
    let openedFor: string | null = null;

    // The surface is going away: forget the panel before closing it, so the close is not reported to
    // a screen that no longer exists. Registered before any panel opens, so it runs before the
    // side-panel service closes the panel for the same reason.
    inject(DestroyRef).onDestroy(() => {
        const closing = panel();
        panel.set(null);
        closing?.close();
    });

    const open = (): void => {
        // Read here, on every open, so a getter on the binding can change the editor between opens.
        const { closed, editor, ...handlers } = binding;

        // Declared before the call: a refused request closes, and reports it, inside `open`.
        let ref: DotSidePanelRef | null = null;

        ref = opener.open(current, {
            ...handlers,
            editor,
            injector,
            closed: () => {
                // Only a panel this surface still shows reports its close. A refused request was
                // never shown; the check below reports it.
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
        // Tracked, so a getter that reads a signal reopens the panel in the editor it now names.
        const editor = binding.editor;

        untracked(() => {
            if (value !== null && value !== undefined) {
                const identity = identityOf(value, editor);
                const showing = panel();

                if (showing && identity !== openedFor) {
                    panel.set(null);
                    showing.close();
                }

                shown.set(value);
                openedFor = identity;

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

/**
 * What a panel was opened for: the content it shows and the editor it shows it in. A change to
 * anything else in the request, such as its title, keeps the panel.
 *
 * @param request What the panel shows.
 * @param editor The editor the binding forces, if any.
 * @returns A key that differs whenever the panel must reopen.
 */
function identityOf(request: DotEditContentRequest, editor: 'legacy' | undefined): string {
    return JSON.stringify([request.mode, request.inode, request.contentType, editor]);
}
