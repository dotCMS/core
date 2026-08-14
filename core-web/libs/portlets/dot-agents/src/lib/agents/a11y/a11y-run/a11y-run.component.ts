import { patchState, signalState } from '@ngrx/signals';

import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAgentRunService } from '@dotcms/data-access';
import { DotPageScannerService } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotA11yActionsComponent } from './a11y-actions/a11y-actions.component';
import { DotA11yActivityComponent } from './a11y-activity/a11y-activity.component';
import { DotA11yPreviewComponent } from './a11y-preview/a11y-preview.component';
import { DotA11yScoreComponent } from './a11y-score/a11y-score.component';

import { DotA11yDiffViewerComponent } from '../a11y-diff/a11y-diff-viewer.component';
import { DotA11yDiffComponent } from '../a11y-diff/a11y-diff.component';
import { A11Y_PAGE_LIST_ROUTE } from '../a11y.constants';
import { StudioPageRow } from '../models/accessibility-studio.models';
import { PageDiffFile } from '../models/page-render-sources.models';
import { DotA11yAgentService } from '../services/dot-a11y-agent.service';
import { A11yRunStore } from '../store/a11y-run.store';

/** The side panel's two accordion panels. */
type StudioPanel = 'scanner' | 'files';

/**
 * This screen's own VIEW state — everything the run itself doesn't own.
 *
 * The run (phase, scans, findings, report) lives in {@link A11yRunStore}; what
 * stays here is only what the user is *looking at*: which panels are open, which
 * file's diff is on the right, and the changed-file count the diff list reports
 * back up. One `signalState` rather than loose `signal()`s so the whole view state
 * is declared in one place and every write goes through `patchState`.
 */
interface A11yRunViewState {
    /**
     * The source file whose diff the right pane is showing, or null for the preview.
     * Set from the changed-files accordion in the left panel; the preview stays
     * mounted underneath so returning to it doesn't reload the iframes.
     */
    diffFile: PageDiffFile | null;

    /**
     * Which of the side panel's accordion panels are open — the `scanner` (score,
     * issues, activity log + scan/fix actions) and `files` (changed files + publish).
     * `p-accordion` is in `multiple` mode, so this is the array it binds: the panels
     * open and close independently and the user can watch a run while reviewing the
     * files it touched. The scanner starts open, files collapsed.
     */
    openPanels: StudioPanel[];

    /** How many source files differ between working and live, for the count badge. */
    changedFileCount: number;
}

/** The view state every run starts from: scanner open, nothing else yet. */
const INITIAL_VIEW_STATE: A11yRunViewState = {
    diffFile: null,
    openPanels: ['scanner'],
    changedFileCount: 0
};

/**
 * The Studio run screen: the agent column (score widget + recipe log +
 * state-driven action footer) beside a live preview pane.
 */
@Component({
    selector: 'dot-a11y-run',
    imports: [
        AccordionModule,
        ButtonModule,
        TooltipModule,
        DotMessagePipe,
        DotA11yActionsComponent,
        DotA11yActivityComponent,
        DotA11yDiffComponent,
        DotA11yDiffViewerComponent,
        DotA11yPreviewComponent,
        DotA11yScoreComponent
    ],
    templateUrl: './a11y-run.component.html',
    styles: [
        `
            /* Make the OPEN scanner panel's content scroll in place (headers stay put)
               instead of the whole accordion scrolling. PrimeNG's collapse animation
               wraps the content in a grid + <p-motion> + wrapper that its [pt] API
               can't reach; those need min-height:0 so the innermost content — which
               carries overflow-y:auto via [pt] — can be bounded and scroll.
               The wrapper also gets overflow:hidden: the inner content's own
               overflow-y:auto would otherwise escape the collapsed (0-height) panel
               and make the whole page scroll. Scoped to this component's accordion. */
            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent {
                min-height: 0;
                /* Clip the content to its grid row. When collapsed PrimeNG animates
                   the row to 0; without this the content overflows that 0-height cell
                   and scrolls the whole page. The open panel scrolls via its inner
                   content's overflow-y (below), not this box. */
                overflow: hidden;
            }

            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent p-motion {
                min-height: 0;
            }

            :host ::ng-deep [data-testid='studio-panels'] .p-accordioncontent-wrapper {
                min-height: 0;
            }

            /* Both the shrinkable grid row (lets content be bounded shorter than its
               natural height) and the scroll live ONLY on the ACTIVE panel. On a
               collapsed panel PrimeNG animates the grid row to 0; leaving the row
               shrinkable or the content scrollable there would let the content escape
               the 0-height cell and scroll the whole page. */
            :host
                ::ng-deep
                [data-testid='studio-panels']
                .p-accordionpanel-active
                .p-accordioncontent {
                grid-template-rows: minmax(0, 1fr);
            }

            :host
                ::ng-deep
                [data-testid='studio-panels']
                .p-accordionpanel-active
                .p-accordioncontent-content {
                min-height: 0;
                overflow-y: auto;
            }
        `
    ],
    // The run store + the services it drives are provided HERE (not at the root),
    // so each run route gets a fresh instance — navigating to a different page
    // recreates the store with clean scan/fix state.
    providers: [A11yRunStore, DotPageScannerService, DotA11yAgentService, DotAgentRunService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Two rows: an `auto` row for the error banner (collapses to 0 when there is no
    // error, since the banner is the only thing in it) over the main content row, which
    // takes the rest. `minmax(0,1fr)` rather than `1fr` so the panes can be bounded
    // shorter than their content and scroll internally.
    host: { class: 'grid h-full min-h-0 grid-cols-[412px_1fr] grid-rows-[auto_minmax(0,1fr)]' }
})
export class DotA11yRunComponent {
    readonly store = inject(A11yRunStore);

    readonly #router = inject(Router);
    readonly #location = inject(Location);

    /**
     * This screen's view state — see {@link A11yRunViewState}. Read through its
     * deep signals (`$state.openPanels()`), written only via `patchState`.
     */
    readonly $state = signalState<A11yRunViewState>(INITIAL_VIEW_STATE);

    /** True when there's something to publish — drives the Publish bar. */
    readonly $hasChangedFiles = computed(() => this.$state.changedFileCount() > 0);

    constructor() {
        // The page list hands the selected row over in the navigation's `state` (the
        // URL carries only its readable path, which can't supply identifier/host/
        // language). Adopt it, or bounce back to the list when there is none — which
        // is what a cold load, refresh, or pasted run URL looks like, since the run
        // route is reachable only THROUGH the list.
        const row = readHandoverRow(this.#location.getState());
        if (row) {
            this.store.openSelectedPage(row);
        } else {
            this.#toPageList();
        }
    }

    /**
     * Navigate to the page-list route.
     *
     * Absolute, NOT relative: the run screen is the `**` route (the page path is
     * multi-segment, so it can't be a single param), and Angular's `..` drops one URL
     * SEGMENT rather than one route level. From `/agents/a11y/about-us/index` a `..`
     * yields `/agents/a11y/about-us`, which matches `**` again — the same component is
     * reused and the screen never changes.
     */
    #toPageList(): void {
        this.#router.navigate([A11Y_PAGE_LIST_ROUTE]).catch(() => {
            // Navigation cancelled (guard or a newer navigation superseded this
            // one). Nothing to recover — the router owns where we ended up.
        });
    }

    /**
     * Back button / "All pages" — return to the page list. No store reset needed: the
     * run store is provided at this component, so navigating away destroys it and
     * the next run starts fresh.
     */
    backToPageList(): void {
        this.#toPageList();
    }

    /**
     * Whether a given side panel is expanded. `p-accordion` renders the panel bodies
     * itself off the same `openPanels` it binds, so nothing in the template needs
     * this — it's the readable way to assert open state from tests.
     */
    isPanelOpen(panel: StudioPanel): boolean {
        return this.$state.openPanels().includes(panel);
    }

    /**
     * The user opened/closed panels from the accordion itself.
     *
     * `p-accordion` binds `value` as a model, but a `signalState` slice is a readonly
     * deep signal — it can't be the target of a `[(value)]` banana box — so the
     * template splits the two-way binding and writes back through here. In `multiple`
     * mode the accordion emits the open panels as an array; anything else (its
     * single-value / cleared shapes) means "nothing open".
     */
    onOpenPanelsChange(value: string | number | string[] | number[] | null | undefined): void {
        patchState(this.$state, {
            openPanels: Array.isArray(value) ? (value as StudioPanel[]) : []
        });
    }

    /**
     * Ensure a panel is open — used to jump the user to the files list. Not a toggle:
     * pressing "Review files" twice must not close the panel it just opened.
     */
    openPanel(panel: StudioPanel): void {
        const current = this.$state.openPanels();
        if (!current.includes(panel)) {
            patchState(this.$state, { openPanels: [...current, panel] });
        }
    }

    /** A file was picked in (or cleared from) the changed-files list. */
    onDiffFileSelected(file: PageDiffFile | null): void {
        patchState(this.$state, { diffFile: file });
    }

    /** The changed-files list reports how many files differ, for the Publish gate. */
    onChangedFilesCount(count: number): void {
        patchState(this.$state, { changedFileCount: count });
    }

    /**
     * Leave the diff view from the right pane's own control. The accordion's
     * highlighted row follows via its `activeFileId` input, so the two stay in sync.
     */
    closeDiff(): void {
        patchState(this.$state, { diffFile: null });
    }

    /**
     * "Apply these changes" (done phase): publish the page. That promotes the whole
     * working version — the page and its changed source files together — since there
     * is no per-file publishing. The changed-files accordion above lists exactly
     * what goes live.
     */
    applyChanges(): void {
        this.store.publish();
    }

    /** Drop this run's working fixes → back to the scanned state. */
    discardChanges(): void {
        this.store.discard();
    }

    runScan(): void {
        this.store.runScan();
    }

    stopScan(): void {
        this.store.stopScan();
    }

    startFix(): void {
        this.store.startFix();
    }

    stopAgent(): void {
        this.store.stopAgent();
    }

    onSkipCssChange(value: boolean): void {
        this.store.setSkipCss(value);
    }
}

/**
 * The page row the list hands over in the navigation's `state`, or null.
 *
 * Validated rather than cast: `history.state` is external input — it survives a reload,
 * and anything can put anything there via `history.pushState`. The three fields checked
 * are the ones the run screen cannot work without: `identifier` targets the fix,
 * `hostId` scopes the scan URL, and `languageId` selects the version to read. A partial
 * object reaching `openSelectedPage` would scan the wrong page, or a nonexistent one,
 * with no error to explain it — bouncing to the list is the recoverable outcome.
 */
function readHandoverRow(state: unknown): StudioPageRow | null {
    if (!state || typeof state !== 'object') {
        return null;
    }

    const row = (state as { row?: unknown }).row;
    if (!row || typeof row !== 'object') {
        return null;
    }

    const { identifier, hostId, languageId } = row as Partial<StudioPageRow>;

    return typeof identifier === 'string' &&
        identifier.length > 0 &&
        typeof hostId === 'string' &&
        hostId.length > 0 &&
        typeof languageId === 'number'
        ? (row as StudioPageRow)
        : null;
}
