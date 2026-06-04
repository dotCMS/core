import { patchState, signalState } from '@ngrx/signals';
import { EMPTY, of } from 'rxjs';

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

import { CheckboxModule } from 'primeng/checkbox';
import { ListboxModule } from 'primeng/listbox';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotWorkflowService } from '@dotcms/data-access';
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
import {
    parseWorkflowToken,
    workflowEntryToToken,
    WorkflowFilterEntry
} from '../../../../utils/functions';

/**
 * One selected scheme, optionally pinned to a single step. `step` omitted means
 * "all steps of this scheme". The `schemeId[:stepId]` token encoding/decoding is
 * shared with the store via {@link parseWorkflowToken} / {@link workflowEntryToToken}.
 */
type WorkflowSelection = WorkflowFilterEntry;

interface State {
    schemes: DotCMSWorkflow[];
    steps: WorkflowStep[];
    loadingSchemes: boolean;
    loadingSteps: boolean;
}

/**
 * Two-column popover filter for the Content Drive toolbar: workflow schemes on
 * the left, steps of the focused scheme on the right. Mirrors the content-type
 * filter visually and in its focus-vs-selection split.
 *
 * Selection model:
 * - Schemes are MULTI-select (checkbox per row).
 * - Steps are SINGLE-select PER SCHEME: each selected scheme can pin one step;
 *   no pinned step means "all steps of that scheme".
 * - The selection is stored as ONE filter key, `workflow: string[]`, where each
 *   entry is `schemeId` or `schemeId:stepId`. Co-locating the step with its
 *   scheme keeps the pairing explicit (no need to infer a step's scheme), so
 *   reconcile and URL restore need no step lookups.
 * - Focus (which scheme's steps show on the right) is separate from selection,
 *   exactly like base-type focus vs its checkbox in the content-type filter.
 * - Cascade: pinning a step selects its (focused) scheme; deselecting a scheme
 *   removes its pinned step.
 *
 * The selection is serialized to the single `workflow` filter key as
 * `schemeId[:stepId]` tokens and round-trips through the URL. The drive-search
 * request applies them server-side as OR-combined SQL (scheme-only entries match
 * by content-type assignment; step-pinned entries match the current task).
 */
@Component({
    selector: 'dot-content-drive-workflow-filter',
    imports: [
        FormsModule,
        CheckboxModule,
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
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

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

    /**
     * Bumped on every `#loadSchemes` call so a late response from a superseded
     * load (e.g. rapid content-type toggles) can't overwrite a newer one.
     */
    #schemesRequestId = 0;

    /** Flat stepId→name lookup over every cached step, for chip labels. */
    readonly #stepNameById = computed(() => {
        const map = new Map<string, string>();
        Object.values(this.#stepCache()).forEach((steps) =>
            steps.forEach((step) => map.set(step.id, step.name))
        );
        return map;
    });

    /** Current selection, parsed from the single `workflow` filter key. */
    readonly $selection = linkedSignal<WorkflowSelection[]>(() => {
        // getFilterValue can return string | string[]; only an array is valid here.
        const raw = this.#store.getFilterValue('workflow');
        return (Array.isArray(raw) ? raw : []).map(parseWorkflowToken);
    });

    /** Scheme whose steps are shown on the right. Separate from selection. */
    readonly $focusedScheme = signal<string | null>(null);

    /** Right-column radio value: the step pinned for the focused scheme, if any. */
    protected readonly $stepModel = computed(() => {
        const focused = this.$focusedScheme();
        if (!focused) return null;
        return this.$selection().find((entry) => entry.scheme === focused)?.step ?? null;
    });

    /**
     * Content-type filter value, read reactively. Returns the raw value (possibly
     * `undefined`) rather than normalizing to `[]`, so the equality check below
     * stays stable — normalizing would mint a new array on every unrelated filter
     * change and re-trigger the scheme reload effect in a loop.
     */
    readonly #contentTypeFilter = computed(() => {
        // Keep the raw array reference (stable identity) or undefined — never mint a
        // fresh `[]`, which would re-trigger the reload effect on every filter change.
        const raw = this.#store.getFilterValue('contentType');
        return Array.isArray(raw) ? raw : undefined;
    });

    /**
     * Empty-schemes message key. With content type(s) selected, the schemes came
     * up empty because those types have no workflows (singular vs plural by
     * count). With none selected, it's the unfiltered "no schemes at all" case.
     */
    protected readonly $noSchemesMessageKey = computed(() => {
        const count = this.#contentTypeFilter()?.length ?? 0;
        if (count === 0) return 'content-drive.workflow-filter.no-schemes';
        return count === 1
            ? 'content-drive.workflow-filter.no-workflows'
            : 'content-drive.workflow-filter.no-workflows.plural';
    });

    /**
     * One chip entry per selected scheme: `<scheme>`, or `<scheme> — <step>` for
     * a scheme with a pinned step. The chip prepends the "Workflow" title.
     */
    readonly $chipSelections = computed<string[]>(() => {
        const schemeCache = this.#schemeCache();
        const stepNames = this.#stepNameById();

        return this.$selection().map(({ scheme, step }) => {
            const name = schemeCache[scheme]?.name ?? scheme;
            const stepName = step ? stepNames.get(step) : null;
            return stepName ? `${name} — ${stepName}` : name;
        });
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

    protected isSchemeSelected(schemeId: string): boolean {
        return this.$selection().some((entry) => entry.scheme === schemeId);
    }

    /** Left-row click: focus the scheme so its steps load on the right. */
    protected onFocusChange(schemeId: string | null): void {
        if (!schemeId || schemeId === this.$focusedScheme()) return;
        this.#focusScheme(schemeId);
    }

    /** Left checkbox: toggle scheme membership; dropping a scheme drops its step. */
    protected onSchemeToggle(schemeId: string): void {
        const selection = this.$selection();
        const next = selection.some((entry) => entry.scheme === schemeId)
            ? selection.filter((entry) => entry.scheme !== schemeId)
            : [...selection, { scheme: schemeId }];

        this.$selection.set(next);
        this.#syncStore();
    }

    /** Right radio: pin/replace the step for the focused scheme (single per scheme). */
    protected onStepChange(stepId: string | null): void {
        const focused = this.$focusedScheme();
        if (!focused) return;

        const selection = this.$selection();
        const others = selection.filter((entry) => entry.scheme !== focused);
        // Pinning a step also selects its scheme; clearing it keeps the scheme.
        const focusedEntry: WorkflowSelection = stepId
            ? { scheme: focused, step: stepId }
            : { scheme: focused };

        this.$selection.set([...others, focusedEntry]);
        this.#syncStore();
    }

    protected onClearAll(): void {
        this.$selection.set([]);
        this.#syncStore();
    }

    #loadSchemes(): void {
        const rawContentTypes = this.#store.getFilterValue('contentType');
        const contentTypes = Array.isArray(rawContentTypes) ? rawContentTypes : [];
        const requestId = ++this.#schemesRequestId;
        patchState(this.$state, { loadingSchemes: true });

        const source$ = contentTypes.length
            ? this.#workflowService.getSchemesByContentTypes(contentTypes)
            : this.#workflowService.get();

        source$
            .pipe(
                take(1),
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    // Only the current request should clear the spinner — a
                    // superseded load's failure must not touch a newer one.
                    if (requestId === this.#schemesRequestId) {
                        patchState(this.$state, { loadingSchemes: false });
                    }
                    // Keep the existing selection: returning EMPTY skips the
                    // subscribe below, so we don't reconcile against an empty list
                    // and silently drop a URL-restored workflow filter on a
                    // transient backend failure.
                    return EMPTY;
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((schemes) => {
                // A newer load (e.g. a later content-type change) supersedes this one.
                if (requestId !== this.#schemesRequestId) return;
                patchState(this.$state, { schemes, loadingSchemes: false });
                this.#cacheSchemes(schemes);
                this.#reconcileSelection(schemes);
            });
    }

    /**
     * After a (re)fetch, drop selections whose scheme no longer exists, focus the
     * first remaining scheme, and load steps for any pinned scheme so the chip can
     * label the step even for schemes the user hasn't focused yet.
     */
    #reconcileSelection(schemes: DotCMSWorkflow[]): void {
        const available = new Set(schemes.map((scheme) => scheme.id));

        const kept = this.$selection().filter((entry) => available.has(entry.scheme));
        if (kept.length !== this.$selection().length) {
            this.$selection.set(kept);
            this.#syncStore();
        }

        const focused = this.$focusedScheme();
        const nextFocus = focused && available.has(focused) ? focused : (kept[0]?.scheme ?? null);
        if (nextFocus) {
            this.#focusScheme(nextFocus);
        } else {
            this.$focusedScheme.set(null);
            patchState(this.$state, { steps: [] });
        }

        // Resolve step names for pinned schemes the user hasn't focused — the
        // focused scheme is already loaded by #focusScheme above, so exclude it
        // to avoid a duplicate getSteps request.
        this.#ensureStepsLoaded(
            kept
                .filter((entry) => entry.step && entry.scheme !== nextFocus)
                .map((entry) => entry.scheme)
        );
    }

    #focusScheme(schemeId: string): void {
        this.$focusedScheme.set(schemeId);

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
                catchError((error) => {
                    this.#httpErrorManager.handle(error);
                    return of([] as WorkflowStep[]);
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((steps) => {
                this.#cacheSteps(schemeId, steps);
                // A late response for a scheme the user moved away from must not
                // overwrite the current right column.
                if (this.$focusedScheme() === schemeId) {
                    patchState(this.$state, { steps, loadingSteps: false });
                }
            });
    }

    /** Loads (and caches) steps for the given schemes so chip labels can resolve. */
    #ensureStepsLoaded(schemeIds: string[]): void {
        schemeIds
            .filter((id) => !this.#stepCache()[id])
            .forEach((id) => {
                this.#workflowService
                    .getSteps(id)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            this.#httpErrorManager.handle(error);
                            return of([] as WorkflowStep[]);
                        }),
                        takeUntilDestroyed(this.#destroyRef)
                    )
                    .subscribe((steps) => {
                        this.#cacheSteps(id, steps);
                        if (this.$focusedScheme() === id) {
                            patchState(this.$state, { steps, loadingSteps: false });
                        }
                    });
            });
    }

    #syncStore(): void {
        const selection = this.$selection();
        if (selection.length) {
            this.#store.patchFilters({ workflow: selection.map(workflowEntryToToken) });
        } else {
            this.#store.removeFilter('workflow');
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
