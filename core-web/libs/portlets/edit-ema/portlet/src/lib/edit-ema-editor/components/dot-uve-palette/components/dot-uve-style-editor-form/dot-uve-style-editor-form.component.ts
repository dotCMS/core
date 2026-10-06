import { EMPTY, merge, Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    share,
    switchMap,
    tap,
    withLatestFrom
} from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { StyleEditorProperties } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/types/internal';

import { UveStyleEditorFieldCheckboxGroupComponent } from './components/uve-style-editor-field-checkbox-group/uve-style-editor-field-checkbox-group.component';
import { UveStyleEditorFieldDropdownComponent } from './components/uve-style-editor-field-dropdown/uve-style-editor-field-dropdown.component';
import { UveStyleEditorFieldInputComponent } from './components/uve-style-editor-field-input/uve-style-editor-field-input.component';
import { UveStyleEditorFieldRadioComponent } from './components/uve-style-editor-field-radio/uve-style-editor-field-radio.component';
import { StyleEditorFormBuilderService } from './services/style-editor-form-builder.service';

import { UveOptimisticSaveService } from '../../../../../services/uve-optimistic-save/uve-optimistic-save.service';
import {
    STYLE_EDITOR_FIELD_TYPES,
    STYLE_EDITOR_INPUT_IDLE_SAVE_TIME,
    STYLE_EDITOR_SAVE_DEBOUNCE_TIME
} from '../../../../../shared/consts';
import { UVE_STATUS } from '../../../../../shared/enums';
import { ActionPayload } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { PageType } from '../../../../../store/models';
import { filterFormValues } from '../../utils';

type SaveResult =
    | { ok: true; isTraditionalPage: boolean }
    | { ok: false; error: { message?: string } | null; isTraditionalPage: boolean };

/**
 * Save target captured at form-change time. Frozen alongside each change so a
 * commit that fires later (idle debounce, blur) can never pick up a contentlet
 * selected after the edit happened.
 */
type SaveContext = {
    activeContentlet: ActionPayload | null | undefined;
    isTraditionalPage: boolean;
};

@Component({
    selector: 'dot-uve-style-editor-form',
    templateUrl: './dot-uve-style-editor-form.component.html',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        AccordionModule,
        ButtonModule,
        UveStyleEditorFieldInputComponent,
        UveStyleEditorFieldDropdownComponent,
        UveStyleEditorFieldCheckboxGroupComponent,
        UveStyleEditorFieldRadioComponent
    ],
    providers: [UveOptimisticSaveService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'block h-full w-full'
    }
})
export class DotUveStyleEditorFormComponent {
    $schema = input.required<StyleEditorFormSchema>({ alias: 'schema' });

    readonly #formBuilder = inject(StyleEditorFormBuilderService);
    readonly #form = signal<FormGroup | null>(null);
    readonly #uveStore = inject(UVEStore);
    readonly #optimisticSave = inject(UveOptimisticSaveService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $sections = computed(() => this.$schema().sections);
    readonly $form = computed(() => this.#form());

    readonly STYLE_EDITOR_FIELD_TYPES = STYLE_EDITOR_FIELD_TYPES;

    /**
     * Commit trigger for continuous (streaming) fields — text/number inputs.
     * Emitted on blur/Enter so a save fires once the user finishes editing,
     * instead of once per keystroke.
     */
    readonly #inputCommit$ = new Subject<void>();

    /**
     * Set of control ids that stream intermediate values while editing (inputs).
     * These are excluded from the per-change save path; they commit on blur/Enter.
     * Anything not in this set is treated as a discrete control and commits on change.
     */
    readonly #continuousFieldIds = computed(() => {
        const ids = new Set<string>();
        this.$schema().sections.forEach((section) =>
            section.fields.forEach((field) => {
                if (field.type === STYLE_EDITOR_FIELD_TYPES.INPUT) {
                    ids.add(field.id);
                }
            })
        );

        return ids;
    });

    /**
     * Tracks only the contentlet identifier so that bounds updates (which call setSelected
     * with the same contentlet but new coordinates) do not cause spurious form rebuilds
     * that would reset the user's in-progress input.
     */
    readonly #selectedContentletId = computed(
        () => this.#uveStore.editorSelected()?.payload?.contentlet?.identifier ?? null
    );

    /**
     * Writable signal for open accordion panels. Initialized to all-open in #buildForm
     * (on contentlet change) and kept in sync via (valueChange) on the template.
     * Using a signal (not computed) prevents schema/section reference changes from
     * creating a new array reference that would force the accordion to reset.
     * Intentionally not re-seeded on schema-only changes — schema is treated as
     * stable per contentlet (it is content-type-derived and does not change mid-session).
     */
    readonly #activeTabIndices = signal<number[]>([]);
    readonly $activeTabIndices = this.#activeTabIndices.asReadonly();

    /**
     * Rebuilds the form when the selected contentlet changes.
     * Tracks only the contentlet identifier — not the full editorSelected signal — so
     * bounds updates (which emit a new editorSelected object for the same contentlet)
     * do not trigger a rebuild and erase the user's in-progress input.
     */
    $reloadSchemaEffect = effect(() => {
        const schema = untracked(() => this.$schema());
        const contentletId = this.#selectedContentletId();

        if (schema && contentletId) {
            untracked(() => this.#buildForm(schema));
        }
    });

    constructor() {
        this.#listenToFormChanges();
    }

    onAccordionChange(indices: number[]): void {
        this.#activeTabIndices.set(indices);
    }

    /**
     * Called when a continuous field (text/number input) finishes editing
     * (blur or Enter). Triggers a single save of the current form value.
     */
    onInputCommit(): void {
        this.#inputCommit$.next();
    }

    /**
     * Builds a form from the schema using the form builder service.
     * Reads initial values from pageAsset (the source of truth) rather than from
     * editorSelected.payload, which is populated from SDK postMessages and may not
     * include persisted dotStyleProperties.
     */
    #buildForm(schema: StyleEditorFormSchema): void {
        this.#activeTabIndices.set(schema.sections.map((_, i) => i));

        const activeContentlet = this.#uveStore.editorSelected()?.payload;

        // pageAsset is already untracked here (called from within untracked() in the effect).
        // extractFromRollback reads the current pageAsset, which always reflects the latest
        // saved or optimistically-updated dotStyleProperties.
        const extracted = activeContentlet
            ? this.#optimisticSave.extractFromRollback(activeContentlet, ['dotStyleProperties'])
            : null;
        const initialValues = extracted?.dotStyleProperties as StyleEditorProperties | undefined;

        // Clear form first so the template destroys the form block and unbinds old controls.
        // Otherwise replacing FormGroup in place leaves stale DOM (e.g. dropdown with formControlName
        this.#form.set(null);
        queueMicrotask(() => {
            const form = this.#formBuilder.buildForm(schema, initialValues);
            this.#form.set(form);
        });
    }

    /**
     * Restores form values from the rolled-back pageAssetResponse state.
     * Used when rollback occurs to sync form with restored state.
     *
     * This method rebuilds the entire form (rather than patching) so
     * #listenToFormChanges starts a fresh per-form stream. The old form's
     * stream stays subscribed (mergeMap) so its pending idle commits still
     * complete — they carry the context captured at change time, so they can
     * only target the contentlet that was actually edited — while its blur
     * path goes inert because the old form is no longer the current $form().
     */
    #restoreFormFromRollback(): void {
        const activeContentlet = this.#uveStore.editorSelected()?.payload;
        const schema = this.$schema();

        if (!activeContentlet || !schema) {
            return;
        }

        try {
            const extracted = this.#optimisticSave.extractFromRollback(activeContentlet, [
                'dotStyleProperties'
            ]);
            const styleProperties = extracted?.dotStyleProperties as StyleEditorProperties;
            const restoredForm = this.#formBuilder.buildForm(schema, styleProperties);
            this.#form.set(restoredForm);
        } catch (error) {
            console.error('Error restoring form from rollback:', error);
        }
    }

    /**
     * Listens to form changes and separates two concerns that used to be fused
     * into a single inactivity-timer debounce:
     *
     * 1. **Live preview** (headless only) — pushes every value change to the
     *    iframe instantly, including each keystroke of a text input.
     * 2. **Persist** — saves on a *commit*, where the commit signal differs by
     *    field type:
     *      - discrete fields (dropdown/radio/checkbox) commit on value change;
     *      - continuous fields (text/number inputs) commit on blur/Enter, or
     *        automatically after an idle pause far longer than any
     *        inter-keystroke gap — so a slow typist never triggers one save
     *        per letter, and no interaction is required to persist.
     *
     * Uses mergeMap to subscribe to each form's valueChanges when the form signal
     * changes (e.g., during rollback restoration). mergeMap keeps all subscriptions
     * active, so pending saves from the old form still complete while the new form
     * is also processed — no flags, timeouts, or manual subscription management.
     *
     * Two guarantees protect against cross-contentlet writes when a form is
     * rebuilt while a commit is pending:
     * - the save target (contentlet + page type) is captured at change time
     *   inside each per-form stream, never re-read after a debounce, so a
     *   pending idle save always targets the contentlet that was being edited
     *   (form values, by contrast, are read at emit time — they belong to the
     *   closed-over form and can never cross contentlets);
     * - blur/Enter commits are gated to the current form, so a stale stream
     *   never reacts to a blur that happened on a newer form.
     */
    #listenToFormChanges(): void {
        // When the form signal changes (rebuilt during rollback), mergeMap subscribes
        // to the new form while keeping the old form's subscription active.
        toObservable(this.$form)
            .pipe(
                filter((form): form is FormGroup => form !== null),
                mergeMap((form) => {
                    const continuousFieldIds = this.#continuousFieldIds();
                    let previousValue = form.getRawValue();

                    // Maps a change to the save payload. The contentlet/page-type
                    // context is the one frozen at change time; the form values are
                    // read at emit time from the closed-over form — always this
                    // form's own values, so reading them live can never cross
                    // contentlets, and it lets distinctUntilChanged dedupe a late
                    // idle commit against a newer discrete commit.
                    const toCommit = ({ context }: { context: SaveContext }) => ({
                        formValues: form.getRawValue(),
                        activeContentlet: context.activeContentlet,
                        isTraditionalPage: context.isTraditionalPage
                    });

                    // Every value change drives the live preview and is classified
                    // so the save path can decide when to commit. The save context
                    // (active contentlet + page type) is frozen HERE, at change
                    // time — never re-read after a debounce — so a selection change
                    // while a commit is pending can never re-target the save to
                    // the wrong contentlet.
                    const changes$ = form.valueChanges.pipe(
                        map(() => {
                            const currentValue = form.getRawValue();
                            const changedKeys = this.#changedFieldKeys(previousValue, currentValue);
                            previousValue = currentValue;

                            const context: SaveContext = {
                                activeContentlet: this.#uveStore.editorSelected()?.payload,
                                isTraditionalPage:
                                    this.#uveStore.pageType() === PageType.TRADITIONAL
                            };

                            return { changedKeys, context };
                        }),
                        filter(({ changedKeys }) => changedKeys.length > 0),
                        // Live preview: headless pushes every change (incl. keystrokes)
                        // to the iframe immediately. Traditional pages cannot do this.
                        tap(({ context }) => {
                            if (!context.isTraditionalPage) {
                                this.#optimisticSave.updateIframeOptimistically(
                                    context.activeContentlet,
                                    { dotStyleProperties: form.getRawValue() }
                                );
                            }
                        }),
                        // All commit branches below consume this stream; share() keeps
                        // a single subscription so the diff/preview run once per change.
                        share()
                    );

                    // Discrete fields (dropdown/radio/checkbox): the change IS the commit.
                    const discreteCommits$ = changes$.pipe(
                        filter(({ changedKeys }) =>
                            changedKeys.some((key) => !continuousFieldIds.has(key))
                        ),
                        map(toCommit)
                    );

                    // Continuous fields (inputs): a keystroke is NOT a commit. The user
                    // finishes either explicitly (blur/Enter → blurCommits$) or
                    // implicitly by pausing — an idle window far longer than any
                    // inter-keystroke gap, so it never fires mid-word. The commit
                    // context was frozen at change time, so even if the selection
                    // moves during this idle window the save still targets the
                    // contentlet that was being edited.
                    const idleInputCommits$ = changes$.pipe(
                        filter(({ changedKeys }) =>
                            changedKeys.some((key) => continuousFieldIds.has(key))
                        ),
                        debounceTime(STYLE_EDITOR_INPUT_IDLE_SAVE_TIME),
                        map(toCommit)
                    );

                    // Blur/Enter commits. #inputCommit$ is component-lifetime while
                    // this inner stream is per-form (mergeMap keeps old inners alive
                    // so their pending saves complete), so:
                    // - gate on the form still being current — a stale inner must
                    //   not react to a blur that happened on a NEWER form;
                    // - withLatestFrom pairs the blur with the context frozen at
                    //   the last change. A blur with no prior change emits nothing
                    //   (there is nothing to save).
                    const blurCommits$ = this.#inputCommit$.pipe(
                        filter(() => this.$form() === form),
                        withLatestFrom(changes$),
                        map(([, change]) => toCommit(change))
                    );

                    // A commit is: a discrete field change, an input idle pause,
                    // or an input signalling it finished editing (blur/Enter).
                    // Every branch carries the context captured at change time.
                    return merge(discreteCommits$, idleInputCommits$, blurCommits$).pipe(
                        // A blur with no net change (e.g. tabbing through a field)
                        // must not hit the API.
                        distinctUntilChanged(
                            (prev, curr) =>
                                JSON.stringify(prev.formValues) === JSON.stringify(curr.formValues)
                        ),
                        // Small window: batches a rapid burst of commits into one save.
                        // NOT a "wait for typing to finish" timer — blur/Enter does that.
                        debounceTime(STYLE_EDITOR_SAVE_DEBOUNCE_TIME),
                        // switchMap cancels the previous in-flight save when a new
                        // commit arrives, so a fast burst resolves to a single save
                        // (and a single toast) for the latest value.
                        switchMap(({ formValues, activeContentlet, isTraditionalPage }) =>
                            this.#performSave(formValues, activeContentlet, isTraditionalPage)
                        )
                    );
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((result) => {
                // 'error' in result narrows the discriminated union reliably
                // across the rxjs/switchMap call site (plain `result.ok` does not
                // always narrow here, depending on operator inference depth).
                if ('error' in result) {
                    // Rollback already happened synchronously in store's error handler,
                    // so we can restore the form immediately.
                    this.#restoreFormFromRollback();

                    if (result.isTraditionalPage) {
                        this.#uveStore.setUveStatus(UVE_STATUS.LOADED);
                    }

                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get(
                            'editpage.content.update.contentlet.error'
                        ),
                        detail: result.error?.message || '',
                        life: 2000
                    });

                    return;
                }

                if (result.isTraditionalPage) {
                    // Re-fetch the page from the backend so the iframe renders
                    // the updated styles. pageReload() handles LOADING→LOADED status.
                    this.#uveStore.pageReload();
                }

                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get('message.content.saved'),
                    detail: this.#dotMessageService.get('message.content.note.already.published'),
                    life: 2000
                });
            });
    }

    /**
     * Returns the top-level field ids whose value changed between two form
     * snapshots. Nested groups (e.g. checkbox groups) are compared structurally,
     * so a change inside the group surfaces as a change to its field id.
     */
    #changedFieldKeys(
        previous: Record<string, unknown>,
        current: Record<string, unknown>
    ): string[] {
        const keys = new Set([...Object.keys(previous), ...Object.keys(current)]);

        return [...keys].filter(
            (key) => JSON.stringify(previous[key]) !== JSON.stringify(current[key])
        );
    }

    /**
     * Wraps `saveStyleEditor` in an inner observable so the outer pipeline can
     * `switchMap` onto it. Returns a single result envelope (`ok`/`error`) so a
     * single subscribe handles both branches without re-throwing.
     *
     * Side effects (`addCurrentPageToHistory`, `setUveStatus(LOADING)`) live
     * inside the factory so they only run when this inner observable is
     * actually subscribed — i.e. the debounced emission survived to reach
     * switchMap.
     */
    #performSave(
        formValues: Record<string, unknown>,
        activeContentlet: ActionPayload | null | undefined,
        isTraditionalPage: boolean
    ): Observable<SaveResult> {
        if (!activeContentlet) {
            return EMPTY;
        }

        const filteredFormValues = filterFormValues(formValues);
        if (Object.keys(filteredFormValues).length === 0) {
            return EMPTY;
        }

        // Save current state to history BEFORE making the API call so rollback
        // on failure restores this exact state.
        this.#uveStore.addCurrentPageToHistory();

        if (isTraditionalPage) {
            this.#uveStore.setUveStatus(UVE_STATUS.LOADING);
        }

        return this.#uveStore
            .saveStyleEditor({
                containerIdentifier: activeContentlet.container.identifier,
                contentletIdentifier: activeContentlet.contentlet.identifier,
                styleProperties: filteredFormValues,
                pageId: activeContentlet.pageId,
                containerUUID: activeContentlet.container.uuid,
                personaTag: this.#uveStore.$pageData().personaTag
            })
            .pipe(
                map((): SaveResult => ({ ok: true, isTraditionalPage })),
                catchError(
                    (error): Observable<SaveResult> =>
                        of({ ok: false, error: error ?? null, isTraditionalPage })
                )
            );
    }
}
