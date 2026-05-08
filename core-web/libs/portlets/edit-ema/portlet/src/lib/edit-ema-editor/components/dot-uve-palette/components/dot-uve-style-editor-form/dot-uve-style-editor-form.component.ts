import { EMPTY, Observable, of, timer } from 'rxjs';

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
    debounce,
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    switchMap,
    tap
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
    STYLE_EDITOR_DEBOUNCE_TIME,
    STYLE_EDITOR_FIELD_TYPES,
    STYLE_EDITOR_TRADITIONAL_DEBOUNCE_TIME
} from '../../../../../shared/consts';
import { UVE_STATUS } from '../../../../../shared/enums';
import { ActionPayload } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { PageType } from '../../../../../store/models';
import { filterFormValues } from '../../utils';

type SaveResult =
    | { ok: true; isTraditionalPage: boolean }
    | { ok: false; error: { message?: string } | null; isTraditionalPage: boolean };

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
     * Tracks only the contentlet identifier so that bounds updates (which call setSelected
     * with the same contentlet but new coordinates) do not cause spurious form rebuilds
     * that would reset the user's in-progress input.
     */
    readonly #selectedContentletId = computed(
        () => this.#uveStore.editorSelected()?.payload?.contentlet?.identifier ?? null
    );

    /**
     * Computed property that returns an array of all section indices to keep all tabs open by default
     */
    $activeTabIndices = computed(() => {
        const sections = this.$sections();
        return sections.map((_, index) => index);
    });

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

    /**
     * Builds a form from the schema using the form builder service.
     * Reads initial values from pageAsset (the source of truth) rather than from
     * editorSelected.payload, which is populated from SDK postMessages and may not
     * include persisted dotStyleProperties.
     */
    #buildForm(schema: StyleEditorFormSchema): void {
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
     * This method rebuilds the entire form (rather than patching) to trigger
     * the switchMap in #listenToFormChanges, which automatically cancels any
     * pending debounced saves from the old form instance.
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
     * Listens to form changes and handles:
     * 1. Immediate updates to iframe (no debounce)
     * 2. Debounced API calls to save style properties
     *
     * Uses mergeMap to subscribe to each form's valueChanges when the form signal changes
     * (e.g., during rollback restoration). mergeMap keeps all subscriptions active, so both
     * old and new forms' valueChanges will be processed. This ensures that pending debounced
     * saves from the old form will still complete, while also processing changes from the new form.
     * This is a clean reactive approach that eliminates the need for flags, timeouts, or
     * manual subscription management.
     */
    #listenToFormChanges(): void {
        // Convert the form signal to an observable
        // When the form signal changes (rebuilt during rollback), mergeMap subscribes to the new form's valueChanges
        // while keeping the old form's subscription active
        toObservable(this.$form)
            .pipe(
                // Filter out null forms
                filter((form): form is FormGroup => form !== null),
                // Merge with the new form's valueChanges
                // mergeMap keeps all inner subscriptions active, so both old and new forms'
                // valueChanges will be processed, including any pending debounced saves
                mergeMap((form) =>
                    form.valueChanges.pipe(
                        distinctUntilChanged(
                            (prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)
                        ),
                        // Capture activeContentlet at the time of form change (before debounce)
                        // and identify the editor mode for this update.
                        map((formValues) => ({
                            formValues,
                            activeContentlet: this.#uveStore.editorSelected()?.payload,
                            isTraditionalPage: this.#uveStore.pageType() === PageType.TRADITIONAL
                        })),
                        tap(({ formValues, activeContentlet, isTraditionalPage }) => {
                            // Traditional pages do not support instant iframe updates.
                            if (!isTraditionalPage) {
                                this.#optimisticSave.updateIframeOptimistically(activeContentlet, {
                                    dotStyleProperties: formValues
                                });
                            }
                        }),
                        // Traditional: 500ms — short window so the iframe reload
                        // following the save still feels responsive.
                        // Headless: 2s — coalesces rapid slider/picker drags.
                        debounce((changeEvent) =>
                            changeEvent.isTraditionalPage
                                ? timer(STYLE_EDITOR_TRADITIONAL_DEBOUNCE_TIME)
                                : timer(STYLE_EDITOR_DEBOUNCE_TIME)
                        ),
                        // switchMap cancels the previous in-flight save when a new
                        // debounced emission arrives, so a fast burst of edits
                        // resolves to a single save (and a single toast) for the
                        // latest value rather than one per change.
                        switchMap(({ formValues, activeContentlet, isTraditionalPage }) =>
                            this.#performSave(formValues, activeContentlet, isTraditionalPage)
                        )
                    )
                ),
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
                containerUUID: activeContentlet.container.uuid
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
