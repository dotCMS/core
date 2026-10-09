import { DOCUMENT } from '@angular/common';
import { DestroyRef, Signal, afterNextRender, inject, signal } from '@angular/core';

import { Drawer } from 'primeng/drawer';

import { DotKeyboardShortcutService, hasOverlayAbove } from '@dotcms/ui';

import { DotSidePanelNavController } from './dot-side-panel-nav.service';

/**
 * Shared by every side panel, so the author's expanded (full-width) choice carries over between
 * them.
 */
const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

/** Best-effort read of the expanded preference; `false` when storage is unavailable. */
function readExpandedPreference(): boolean {
    try {
        return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
        return false;
    }
}

/** Persists the expanded preference; storage failures must not break the panel. */
function writeExpandedPreference(expanded: boolean): void {
    try {
        localStorage.setItem(EXPANDED_STORAGE_KEY, String(expanded));
    } catch {
        // best-effort: quota errors / disabled storage are ignored.
    }
}

/** What a side panel tells {@link injectSidePanelChrome} about itself. */
export interface DotSidePanelChromeOptions {
    /** The panel itself: its identity on the side-panel stack. */
    panel: object;
    /** The panel's own drawer, for its mask and its container. */
    drawer: Signal<Drawer | undefined>;
    /** The panel's close intent, guard included. Called on Escape and on a click on its mask. */
    requestClose: () => void;
    /** i18n key naming the Escape shortcut, for the shortcut documentation. */
    escapeLabel: string;
}

/** The behaviour every side panel shares, as {@link injectSidePanelChrome} returns it. */
export interface DotSidePanelChrome {
    /** Whether the panel spans the full width (vs 80%), seeded from the stored choice. */
    readonly expanded: Signal<boolean>;
    /** Flips the full-width state and remembers it for the next panel. */
    toggleExpanded(): void;
    /**
     * Escape, as the panel handles it. Exposed for keys the shortcut registry cannot see, such as
     * Escape pressed inside an iframe the panel hosts.
     *
     * @returns `true`: the key is consumed.
     */
    escape(): boolean;
}

/**
 * Gives a side panel the behaviour all side panels share, so each one only writes its own content
 * and its own close guard:
 *
 * - **Escape** closes it, through the shortcut registry rather than a listener of its own: two
 *   document listeners cannot arbitrate (`preventDefault` does not stop the other one), so the
 *   portlet behind would act on the same key. Escape is always consumed while the panel is open, and
 *   only closes when no overlay sits above the panel and it is the frontmost one.
 * - **A click on its own mask** closes it, through the same close intent. The drawer's
 *   `dismissible` must stay off: it hides the drawer the moment the mask is clicked, skipping the
 *   guard and desyncing the one-way `[visible]` binding. The mask is matched by identity, not by its
 *   `p-drawer-mask` class, which every modal drawer shares (the UVE block editor sidebar is one); a
 *   click inside the panel, or a drag that starts inside and ends on the mask, resolves to another
 *   node and is ignored.
 * - **The side-panel stack**: joined after the first render (which collapses the main navigation)
 *   and left on destroy.
 * - **Full width**: the expanded preference, shared by every side panel.
 *
 * Call it from the panel's constructor or a field initializer: it needs an injection context, and
 * everything it registers is released when the panel is destroyed.
 *
 * @param options what the panel tells it about itself
 */
export function injectSidePanelChrome({
    panel,
    drawer,
    requestClose,
    escapeLabel
}: DotSidePanelChromeOptions): DotSidePanelChrome {
    const navController = inject(DotSidePanelNavController);
    const shortcuts = inject(DotKeyboardShortcutService);
    const document = inject(DOCUMENT);
    const expanded = signal(readExpandedPreference());

    const escape = (): boolean => {
        // An overlay above owns the key. Consumed, not declined, so it cannot reach the portlet.
        if (hasOverlayAbove(drawer()?.container)) {
            return true;
        }

        if (navController.isTop(panel)) {
            requestClose();
        }

        return true;
    };

    // Document-wide because `appendTo="body"` moves the drawer and its mask out of the panel's DOM,
    // so no template binding can hear the mask click.
    const onDocumentClick = (event: MouseEvent): void => {
        if (event.target === drawer()?.mask && navController.isTop(panel)) {
            requestClose();
        }
    };

    const withdrawEscape = shortcuts.register({
        combination: 'escape',
        label: escapeLabel,
        handler: escape
    });
    document.addEventListener('click', onDocumentClick);

    // After the current render, so the store mutation it causes does not land during it.
    afterNextRender(() => navController.acquire(panel));

    inject(DestroyRef).onDestroy(() => {
        navController.release(panel);
        withdrawEscape();
        document.removeEventListener('click', onDocumentClick);
    });

    return {
        expanded: expanded.asReadonly(),
        toggleExpanded: () => {
            const next = !expanded();
            expanded.set(next);
            writeExpandedPreference(next);
        },
        escape
    };
}
