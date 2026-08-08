import { inject, Injectable } from '@angular/core';

import { GlobalStore } from '@dotcms/store';

/**
 * sessionStorage key remembering the main navigation's collapsed state from BEFORE the first side
 * panel opened, so it can be restored when the last panel closes. `sessionStorage` (not
 * `localStorage`) on purpose: it survives an in-page refresh while a panel is open (e.g. a shared
 * `?editContent=` link, same tab), but is cleared for a genuinely new session — so a key left
 * behind by a tab that closed/crashed before `release()` could clear it self-clears instead of
 * making the next session wrongly expand a nav the user had chosen to keep collapsed.
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

/** Collapse the navs for the side panel when the viewport is narrower than this (px). */
const COLLAPSE_MAX_WIDTH = 1800;

/**
 * Collapses the main navigation (logo + menus) while an Edit Content side panel is open, and
 * restores it when the last one closes — but only if it was expanded to begin with (a nav the user
 * had already collapsed stays collapsed), and only on small viewports ({@link shouldCollapse}).
 *
 * Ref-counted because the relationship field can stack a second panel on top: the nav must stay
 * collapsed until EVERY panel has closed, not just the top one. The pre-panel state is kept in
 * sessionStorage so it is remembered across a refresh that happens while a panel is open, but not
 * leaked into a new session (see {@link PREV_NAV_COLLAPSED_KEY}).
 */
@Injectable({ providedIn: 'root' })
export class DotSidePanelNavController {
    readonly #globalStore = inject(GlobalStore);

    /**
     * Open panels in open order; the last entry is the frontmost (top-of-stack) panel. An ordered
     * stack rather than a bare counter so {@link isTop} can tell which panel a document-level ESC
     * should close (only the frontmost, not the whole stack).
     */
    #stack: object[] = [];

    /** Whether the viewport is narrow enough ({@link COLLAPSE_MAX_WIDTH}) to collapse the navs. */
    shouldCollapse(): boolean {
        return window.innerWidth < COLLAPSE_MAX_WIDTH;
    }

    /**
     * Call when a side panel opens, passing the panel as `token`. On the first open — and only on a
     * small enough viewport ({@link shouldCollapse}) — collapses the nav, remembering its previous
     * state. On wider screens it does nothing (so `release` also becomes a no-op, since no state is
     * saved).
     */
    acquire(token: object): void {
        if (this.#stack.length === 0 && this.shouldCollapse()) {
            // Write-if-absent: a refresh while a panel is open must not overwrite the ORIGINAL
            // pre-panel state with the (already-collapsed) current one.
            if (readPrevNavCollapsed() === null) {
                writePrevNavCollapsed(this.#globalStore.isNavigationCollapsed());
            }

            if (!this.#globalStore.isNavigationCollapsed()) {
                this.#globalStore.collapseNavigation();
            }
        }

        this.#stack.push(token);
    }

    /** Call when a side panel closes, passing the same `token`. Restores the nav once the last panel has closed. */
    release(token: object): void {
        const index = this.#stack.lastIndexOf(token);
        if (index > -1) {
            this.#stack.splice(index, 1);
        }

        if (this.#stack.length === 0) {
            // Restore only if the nav was expanded before the panel collapsed it.
            if (readPrevNavCollapsed() === false) {
                this.#globalStore.expandNavigation();
            }

            clearPrevNavCollapsed();
        }
    }

    /**
     * Whether `token` is the frontmost (top-of-stack) open panel — the only one a document-level ESC
     * should close. Returns `false` for panels beneath it, so a single ESC closes one panel at a time.
     */
    isTop(token: object): boolean {
        return this.#stack.length > 0 && this.#stack[this.#stack.length - 1] === token;
    }
}
