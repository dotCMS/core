import { Component, computed, DestroyRef, inject, signal, viewChild } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';

import { DotExperimentsPanelStore } from '@dotcms/portlets/dot-experiments/data-access';
import { DotKeyboardShortcutService, DotMessagePipe, hasOverlayAbove } from '@dotcms/ui';

import { PANEL_EXPANDED_WIDTH, PANEL_WIDTH } from '../shared/constants';

/** localStorage key persisting the editor's expanded (wide) preference for this panel. */
const EXPANDED_STORAGE_KEY = 'dot-experiments-panel-expanded';

/**
 * Reads the persisted expanded preference. Best-effort: returns `false` when storage is
 * unavailable (SSR/tests) or the value is missing, so a read failure never breaks the panel.
 */
function readExpandedPreference(): boolean {
    try {
        return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
        return false;
    }
}

/** Persists the expanded preference; silently ignores storage failures. */
function writeExpandedPreference(expanded: boolean): void {
    try {
        localStorage.setItem(EXPANDED_STORAGE_KEY, String(expanded));
    } catch {
        // best-effort: quota errors / disabled storage must not break the panel.
    }
}

/**
 * The page's experiments, beside the page (#37478).
 *
 * ## This deliberately mirrors `DotEditContentSidePanelComponent`
 *
 * The drawer chrome below — expand/collapse with a persisted preference, Escape through the
 * shared shortcut registry, click-outside through the drawer's own mask, the transparent mask and
 * the width transition — is **copied on purpose** from
 * `libs/edit-content/src/lib/components/dot-edit-content-side-panel`. It is not convergent
 * evolution and it is not an accident.
 *
 * Each of those four behaviours encodes a bug that was already paid for once, and each is wrong
 * in a way that is invisible until someone hits it:
 *
 * - **Click-outside compares against `this.$drawer().mask`, never `.p-drawer-mask`.** That class
 *   is shared by every modal drawer in the app, so a class check lets another drawer's mask close
 *   this panel.
 * - **Escape goes through {@link DotKeyboardShortcutService}, not a document listener.** Two
 *   independent document listeners cannot arbitrate — `preventDefault` does not stop the other —
 *   so the surface behind would act on the same keypress that closed this panel. The registry
 *   gives the key to the most recent claim.
 * - **{@link hasOverlayAbove} consumes Escape rather than declining it** when something is above,
 *   so the key does not fall through to the editor.
 * - **The storage reads and writes are best-effort**, because a disabled-storage browser must not
 *   break the panel.
 *
 * **Do not improve one of these two panels alone.** They are two copies of one contract, kept in
 * step by hand until they are unified. The extraction is scoped and deliberately deferred: the
 * natural moment is when this panel gains its unsaved-changes veto in phase 2, which is the same
 * problem Edit Content already solved — at that point both consumers need the same non-trivial
 * behaviour and the seam is real. What moves then is the chrome only: the drawer config, the
 * expand/collapse pair, the two arbitration rules, and a `closeRequested` output each consumer can
 * veto. `DotSidePanelNavController` is the piece that makes it more than a file move.
 *
 * ## Where it differs, and why
 *
 * Edit Content is a full editing surface, so blocking what is behind it is correct. This panel
 * only reports — and the editor is allowed to navigate to another page while it is open, at which
 * point the panel re-scopes to that page (D10, FR-034). The mask is therefore transparent and
 * click-through is delivered by closing on it, not by suppressing it.
 *
 * One surface at a time — list, creation, configuration or results, never two (spec Assumptions).
 * Only the list is wired so far; the other three arrive with their own phases, and results behind
 * a nested `@defer` so the charting dependency loads only when results are opened (FR-025f).
 */
@Component({
    selector: 'dot-experiments-panel',
    imports: [DrawerModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-experiments-panel.component.html',
    // Click-outside closes the panel, bound at document level because `appendTo="body"` moves the
    // drawer (and its mask) out of this component's DOM subtree, so a template listener would
    // never receive it. Same reasoning as the Edit Content panel.
    host: {
        '(document:click)': 'onMaskClick($event)'
    }
})
export class DotExperimentsPanelComponent {
    protected readonly store = inject(DotExperimentsPanelStore);
    readonly #shortcuts = inject(DotKeyboardShortcutService);

    /**
     * This panel's own drawer. Needed for its `mask` element: {@link onMaskClick} compares the
     * clicked node against THIS mask rather than against the shared `p-drawer-mask` class, so a
     * mask belonging to another drawer never closes this panel.
     */
    protected readonly $drawer = viewChild(Drawer);

    protected readonly PANEL_WIDTH = PANEL_WIDTH;
    protected readonly PANEL_EXPANDED_WIDTH = PANEL_EXPANDED_WIDTH;

    /**
     * Whether the panel is widened past the sidebar width. Seeded from the editor's persisted
     * preference, so it opens in the mode last chosen.
     */
    protected readonly $expanded = signal(readExpandedPreference());

    protected readonly $width = computed(() =>
        this.$expanded() ? PANEL_EXPANDED_WIDTH : PANEL_WIDTH
    );

    /**
     * The heading, which names the view rather than restating "Experiments" four times.
     *
     * The list's heading is the panel's own title; the deeper views name themselves.
     */
    protected readonly $titleKey = computed<string>(() => {
        switch (this.store.view()) {
            case 'create':
                return 'experiments.configure.header.new-experiment';
            case 'configure':
                return 'experiment.container.configuration.title';
            case 'results':
                return 'experiment.container.report.title';
            default:
                return 'experiments.panel.title';
        }
    });

    constructor() {
        // Escape through the shared registry rather than a listener of this component's own — see
        // the class docs. The claim is withdrawn when the component is destroyed, which is on
        // every close, because the shell mounts and destroys this panel rather than hiding it.
        const withdraw = this.#shortcuts.register({
            combination: 'escape',
            label: 'experiments.panel.shortcut.close',
            handler: () => this.#onEscape()
        });

        inject(DestroyRef).onDestroy(withdraw);
    }

    /**
     * Dismissal, as opposed to the panel closing because the editor left to see a variant.
     *
     * Routed through the store's `close()` so the two stay distinguishable: `close()` discards the
     * view state, `suspendForVariant()` keeps it (FR-039 vs FR-023).
     *
     * Phase 2 puts the unsaved-changes confirm in front of this (FR-019, T062) — which is the
     * point at which this panel and Edit Content's want the same `closeRequested` seam.
     */
    protected requestClose(): void {
        this.store.close();
    }

    protected toggleExpanded(): void {
        const next = !this.$expanded();
        this.$expanded.set(next);
        writeExpandedPreference(next);
    }

    /** Closes on a click on **this** drawer's mask; ignores every other click in the document. */
    protected onMaskClick(event: MouseEvent): void {
        if (event.target !== this.$drawer()?.mask) {
            return;
        }

        this.requestClose();
    }

    /**
     * Escape. Consumed either way, so it never reaches the editor behind — an overlay above owns
     * the key, and otherwise this panel does.
     */
    #onEscape(): boolean {
        if (hasOverlayAbove(this.$drawer()?.container)) {
            return true;
        }

        this.requestClose();

        return true;
    }
}
