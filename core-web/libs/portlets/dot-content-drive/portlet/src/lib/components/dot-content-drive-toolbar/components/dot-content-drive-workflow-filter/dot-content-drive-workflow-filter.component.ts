import { patchState, signalState } from '@ngrx/signals';
import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    linkedSignal,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { catchError, take } from 'rxjs/operators';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotCMSWorkflow, WorkflowStep } from '@dotcms/dotcms-models';
import {
    CHIP_FILTER_LISTBOX_PT,
    CHIP_FILTER_POPOVER_PT,
    DotChipFilterComponent,
    DotFilterListItemComponent
} from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { PANEL_SCROLL_HEIGHT } from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

interface State {
    schemes: DotCMSWorkflow[];
    steps: WorkflowStep[];
    loadingSchemes: boolean;
    loadingSteps: boolean;
}

/**
 * Two-column popover filter for the Content Drive toolbar: workflow schemes on
 * the left, steps of the focused scheme on the right. Mirrors the content-type
 * filter visually, but single-select on both columns.
 *
 * Single-select is intentionally stored as an array of (at most) one element so
 * a future multi-select migration is trivial — the store keys, decoders, and
 * `#syncStore` already speak `string[]`.
 *
 * NOTE: backend search support is tracked in dotCMS/core#35470. Until it ships,
 * this filter renders, persists its selection, drives the chip + Clear-all, and
 * round-trips in the URL, but does NOT change the listing results yet.
 */
@Component({
    selector: 'dot-content-drive-workflow-filter',
    imports: [
        FormsModule,
        ListboxModule,
        PopoverModule,
        RadioButtonModule,
        DotChipFilterComponent,
        DotFilterListItemComponent,
        DotMessagePipe
    ],
    providers: [DotWorkflowService],
    templateUrl: './dot-content-drive-workflow-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveWorkflowFilterComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #workflowService = inject(DotWorkflowService);

    protected readonly listboxPt = CHIP_FILTER_LISTBOX_PT;
    protected readonly popoverPt = CHIP_FILTER_POPOVER_PT;
    protected readonly LISTBOX_SCROLL_HEIGHT = PANEL_SCROLL_HEIGHT;

    readonly $state = signalState<State>({
        schemes: [],
        steps: [],
        loadingSchemes: true,
        loadingSteps: false
    });

    /** Monotonic id→scheme cache so the chip can resolve names across refetches. */
    readonly #schemeCache = signal<Record<string, DotCMSWorkflow>>({});
    /** Monotonic schemeId→steps cache; steps are loaded on demand per scheme. */
    readonly #stepCache = signal<Record<string, WorkflowStep[]>>({});

    /** Selected scheme id(s). Single-select today → length 0 or 1. */
    readonly $selectedScheme = linkedSignal<string[]>(
        () => (this.#store.getFilterValue('workflowScheme') as string[]) ?? []
    );

    /** Selected step id(s). Single-select today → length 0 or 1. */
    readonly $selectedStep = linkedSignal<string[]>(
        () => (this.#store.getFilterValue('workflowStep') as string[]) ?? []
    );

    /** ngModel value for the left (scheme) listbox. */
    protected readonly $schemeModel = computed(() => this.$selectedScheme()[0] ?? null);
    /** ngModel value for the right (step) listbox. */
    protected readonly $stepModel = computed(() => this.$selectedStep()[0] ?? null);

    /**
     * Content-type filter value, read reactively. Returns the raw value (possibly
     * `undefined`) rather than normalizing to `[]`, so the equality check below
     * stays stable — normalizing would mint a new array on every unrelated filter
     * change and re-trigger the scheme reload effect in a loop.
     */
    readonly #contentTypeFilter = computed(
        () => this.#store.getFilterValue('contentType') as string[] | undefined
    );

    /**
     * Chip label: `<scheme>` when no step is chosen, `<scheme> — <step>` otherwise.
     * The chip itself prepends the "Workflow" title.
     */
    readonly $chipSelections = computed<string[]>(() => {
        const schemeId = this.$selectedScheme()[0];
        if (!schemeId) return [];

        const scheme = this.#schemeCache()[schemeId];
        const schemeName = scheme?.name ?? schemeId;

        const stepId = this.$selectedStep()[0];
        if (!stepId) return [schemeName];

        const stepName = (this.#stepCache()[schemeId] ?? this.$state.steps()).find(
            (step) => step.id === stepId
        )?.name;

        return [stepName ? `${schemeName} — ${stepName}` : schemeName];
    });

    constructor() {
        // Loads schemes on init and whenever the content-type selection changes
        // (cases: no content types → all schemes; content types selected →
        // schemes for those content types; base-type-only falls through to all).
        effect(() => {
            this.#contentTypeFilter();
            untracked(() => this.#loadSchemes());
        });
    }

    protected onSchemeChange(schemeId: string | null): void {
        if (!schemeId) {
            this.onClearAll();
            return;
        }

        if (this.$selectedScheme()[0] === schemeId) return;

        this.$selectedScheme.set([schemeId]);
        // Switching scheme invalidates any step from the previous scheme.
        this.$selectedStep.set([]);
        this.#syncStore();
        this.#focusScheme(schemeId);
    }

    protected onStepChange(stepId: string | null): void {
        this.$selectedStep.set(stepId ? [stepId] : []);
        this.#syncStore();
    }

    protected onClearAll(): void {
        this.$selectedScheme.set([]);
        this.$selectedStep.set([]);
        patchState(this.$state, { steps: [] });
        this.#syncStore();
    }

    #loadSchemes(): void {
        const contentTypes = (this.#store.getFilterValue('contentType') as string[]) ?? [];
        patchState(this.$state, { loadingSchemes: true });

        const source$ = contentTypes.length
            ? this.#workflowService.getSchemesByContentTypes(contentTypes)
            : this.#workflowService.get();

        source$
            .pipe(
                take(1),
                catchError(() => of([] as DotCMSWorkflow[])),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((schemes) => {
                patchState(this.$state, { schemes, loadingSchemes: false });
                this.#cacheSchemes(schemes);
                this.#reconcileSelection(schemes);
            });
    }

    /**
     * Keep the current scheme selection only if it still exists in the new
     * scheme set; otherwise clear it (and its step). When kept, (re)load its
     * steps so the right column and chip stay resolved.
     */
    #reconcileSelection(schemes: DotCMSWorkflow[]): void {
        const selectedId = this.$selectedScheme()[0];
        if (!selectedId) return;

        if (schemes.some((scheme) => scheme.id === selectedId)) {
            this.#focusScheme(selectedId);
        } else {
            this.$selectedScheme.set([]);
            this.$selectedStep.set([]);
            patchState(this.$state, { steps: [] });
            this.#syncStore();
        }
    }

    #focusScheme(schemeId: string): void {
        const cached = this.#stepCache()[schemeId];
        if (cached) {
            patchState(this.$state, { steps: cached, loadingSteps: false });
            return;
        }

        patchState(this.$state, { steps: [], loadingSteps: true });
        this.#workflowService
            .getSteps(schemeId)
            .pipe(
                take(1),
                catchError(() => of([] as WorkflowStep[])),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((steps) => {
                // A late response for a scheme the user moved away from must not
                // overwrite the current right column.
                if (this.$selectedScheme()[0] !== schemeId) return;
                patchState(this.$state, { steps, loadingSteps: false });
                this.#cacheSteps(schemeId, steps);
            });
    }

    #syncStore(): void {
        const scheme = this.$selectedScheme();
        const step = this.$selectedStep();

        if (scheme.length) {
            this.#store.patchFilters({ workflowScheme: scheme });
        } else {
            this.#store.removeFilter('workflowScheme');
        }

        if (step.length) {
            this.#store.patchFilters({ workflowStep: step });
        } else {
            this.#store.removeFilter('workflowStep');
        }
    }

    #cacheSchemes(schemes: DotCMSWorkflow[]): void {
        if (!schemes.length) return;
        this.#schemeCache.update((cache) => {
            const next = { ...cache };
            schemes.forEach((scheme) => (next[scheme.id] = scheme));
            return next;
        });
    }

    #cacheSteps(schemeId: string, steps: WorkflowStep[]): void {
        this.#stepCache.update((cache) => ({ ...cache, [schemeId]: steps }));
    }
}
