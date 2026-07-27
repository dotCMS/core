import { inject, Injectable } from '@angular/core';

import { GlobalStore } from '@dotcms/store';

/**
 * localStorage key remembering the main navigation's collapsed state from BEFORE the first side
 * panel opened, so it can be restored when the last panel closes — and survive a hard refresh
 * while a panel is open (e.g. a shared `?editContent=` link).
 */
const PREV_NAV_COLLAPSED_KEY = 'dot-edit-content-side-panel-prev-nav-collapsed';

function readPrevNavCollapsed(): boolean | null {
    try {
        const raw = localStorage.getItem(PREV_NAV_COLLAPSED_KEY);

        return raw === null ? null : raw === 'true';
    } catch {
        return null;
    }
}

function writePrevNavCollapsed(collapsed: boolean): void {
    try {
        localStorage.setItem(PREV_NAV_COLLAPSED_KEY, String(collapsed));
    } catch {
        // best-effort: storage failures must not break the panel.
    }
}

function clearPrevNavCollapsed(): void {
    try {
        localStorage.removeItem(PREV_NAV_COLLAPSED_KEY);
    } catch {
        // best-effort.
    }
}

/** Collapse the navs for the side panel when the viewport is narrower than this (px). */
const COLLAPSE_MAX_WIDTH = 1800;

/**
 * Collapses the main navigation (logo + menus) while an Edit Content side panel is open, and
 * restores it when the last one closes — but only if it was expanded to begin with (a nav the user
 * had already collapsed stays collapsed), and only on small viewports ({@link shouldCollapse}).
 *
 * Ref-counted because the relationship field can stack a second panel on top: the nav must stay
 * collapsed until EVERY panel has closed, not just the top one. The pre-panel state is kept in
 * localStorage so it is remembered even across a refresh that happens while a panel is open.
 */
@Injectable({ providedIn: 'root' })
export class DotSidePanelNavController {
    readonly #globalStore = inject(GlobalStore);
    #openCount = 0;

    /** Whether the viewport is narrow enough ({@link COLLAPSE_MAX_WIDTH}) to collapse the navs. */
    shouldCollapse(): boolean {
        return window.innerWidth < COLLAPSE_MAX_WIDTH;
    }

    /**
     * Call when a side panel opens. On the first open — and only on a small enough viewport
     * ({@link shouldCollapse}) — collapses the nav, remembering its previous state. On wider
     * screens it does nothing (so `release` also becomes a no-op, since no state is saved).
     */
    acquire(): void {
        if (this.#openCount === 0 && this.shouldCollapse()) {
            // Write-if-absent: a refresh while a panel is open must not overwrite the ORIGINAL
            // pre-panel state with the (already-collapsed) current one.
            if (readPrevNavCollapsed() === null) {
                writePrevNavCollapsed(this.#globalStore.isNavigationCollapsed());
            }

            if (!this.#globalStore.isNavigationCollapsed()) {
                this.#globalStore.collapseNavigation();
            }
        }

        this.#openCount++;
    }

    /** Call when a side panel closes. Restores the nav once the last panel has closed. */
    release(): void {
        this.#openCount = Math.max(0, this.#openCount - 1);

        if (this.#openCount === 0) {
            // Restore only if the nav was expanded before the panel collapsed it.
            if (readPrevNavCollapsed() === false) {
                this.#globalStore.expandNavigation();
            }

            clearPrevNavCollapsed();
        }
    }
}
