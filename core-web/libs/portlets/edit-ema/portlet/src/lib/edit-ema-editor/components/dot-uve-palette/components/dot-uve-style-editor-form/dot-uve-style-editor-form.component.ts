import { of, timer } from 'rxjs';

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

import { debounce, distinctUntilChanged, filter, map, mergeMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSPageAsset, StyleEditorProperties } from '@dotcms/types';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { UveStyleEditorFieldCheckboxGroupComponent } from './components/uve-style-editor-field-checkbox-group/uve-style-editor-field-checkbox-group.component';
import { UveStyleEditorFieldDropdownComponent } from './components/uve-style-editor-field-dropdown/uve-style-editor-field-dropdown.component';
import { UveStyleEditorFieldInputComponent } from './components/uve-style-editor-field-input/uve-style-editor-field-input.component';
import { UveStyleEditorFieldRadioComponent } from './components/uve-style-editor-field-radio/uve-style-editor-field-radio.component';
import { StyleEditorFormBuilderService } from './services/style-editor-form-builder.service';

import { UveIframeMessengerService } from '../../../../../services/iframe-messenger/uve-iframe-messenger.service';
import { STYLE_EDITOR_DEBOUNCE_TIME, STYLE_EDITOR_FIELD_TYPES } from '../../../../../shared/consts';
import { UVE_STATUS } from '../../../../../shared/enums';
import { ActionPayload } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { PageType } from '../../../../../store/models';
import {
    extractContentletPropertiesFromPageAsset,
    updateContentletPropertiesInPageAsset,
    filterFormValues
} from '../../utils';

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
    readonly #iframeMessenger = inject(UveIframeMessengerService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $sections = computed(() => this.$schema().sections);
    readonly $form = computed(() => this.#form());

    readonly STYLE_EDITOR_FIELD_TYPES = STYLE_EDITOR_FIELD_TYPES;

    /**
     * Computed property that returns an array of all section indices to keep all tabs open by default
     */
    $activeTabIndices = computed(() => {
        const sections = this.$sections();
        return sections.map((_, index) => index);
    });

    /**
     * Effect that (by design) only runs once, using `untracked()` to read the style editor form schema
     * without subscribing to future changes. This allows the form to be (re)built a single time
     * in reaction to schema input, ensuring no further re-execution even if the schema changes.
     * Intended for one-time initialization rather than reactive synchronization.
     */
    $reloadSchemaEffect = effect(() => {
        const schema = untracked(() => this.$schema());

        if (schema) {
            this.#buildForm(schema);
        }
    });

    constructor() {
        this.#listenToFormChanges();
    }

    /**
     * Builds a form from the schema using the form builder service
     */
    #buildForm(schema: StyleEditorFormSchema): void {
        const activeContentlet = this.#uveStore.editorActiveContentlet();

        // Get styleProperties directly from the contentlet payload (already in the postMessage)
        const initialValues = activeContentlet?.contentlet?.dotStyleProperties;

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
        const activeContentlet = this.#uveStore.editorActiveContentlet();
        const schema = this.$schema();

        if (!activeContentlet || !schema) {
            return;
        }

        try {
            // Use the internal pageAssetResponse signal directly (it's already been rolled back)
            // This ensures we get the rolled-back state, not the computed wrapper
            const rolledBackPage = this.#uveStore.pageAsset();

            if (!rolledBackPage) {
                return;
            }

            const rolledBackAsset = { ...rolledBackPage } as DotCMSPageAsset & {
                content?: unknown;
                requestMetadata?: unknown;
                clientResponse?: unknown;
            };
            delete rolledBackAsset.content;
            delete rolledBackAsset.requestMetadata;
            delete rolledBackAsset.clientResponse;
            // Extract style properties from the rolled-back state using utility function
            const extracted = extractContentletPropertiesFromPageAsset(
                rolledBackAsset,
                activeContentlet,
                ['dotStyleProperties']
            );
            const styleProperties = extracted?.dotStyleProperties as StyleEditorProperties;

            // Rebuild the ENTIRE form with rolled-back values
            // This causes the #form signal to change, which triggers switchMap in #listenToFormChanges
            // to cancel the old subscription (including any pending debounced saves)
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
                            activeContentlet: this.#uveStore.editorActiveContentlet(),
                            isTraditionalPage: this.#uveStore.pageType() === PageType.TRADITIONAL
                        })),
                        tap(({ formValues, activeContentlet, isTraditionalPage }) => {
                            // Traditional pages do not support instant iframe updates.
                            if (!isTraditionalPage) {
                                this.#updateIframeOptimistically(formValues, activeContentlet);
                            }
                        }),
                        // Traditional: emit immediately (of(0) completes right away).
                        // Headless: debounce 2s - timer resets on each new form change.
                        debounce((changeEvent) =>
                            changeEvent.isTraditionalPage
                                ? of(0)
                                : timer(STYLE_EDITOR_DEBOUNCE_TIME)
                        )
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ formValues, activeContentlet, isTraditionalPage }) => {
                this.#saveStyleProperties(formValues, activeContentlet, isTraditionalPage);
            });
    }

    /**
     * Immediately updates the iframe with new form values (no debounce)
     * Uses optimistic updates WITHOUT saving to history (history is saved only on API calls)
     */
    #updateIframeOptimistically(
        formValues: Record<string, unknown>,
        activeContentlet: ActionPayload | null
    ): void {
        if (!activeContentlet) {
            return;
        }

        try {
            // Get the internal pageAssetResponse for optimistic update
            const internalPage = this.#uveStore.pageAsset();
            if (!internalPage) {
                return;
            }

            const internalAsset = { ...internalPage } as DotCMSPageAsset & {
                content?: unknown;
                requestMetadata?: unknown;
                clientResponse?: unknown;
            };
            delete internalAsset.content;
            delete internalAsset.requestMetadata;
            delete internalAsset.clientResponse;
            // Deep clone the pageAssetResponse before mutating to prevent affecting history entries
            // This ensures that mutations don't affect the stored state in history
            const clonedResponse = structuredClone(internalAsset);

            // Update the cloned response (mutates the clone in place)
            const updatedInternalResponse = updateContentletPropertiesInPageAsset(
                clonedResponse,
                activeContentlet,
                {
                    dotStyleProperties: formValues
                }
            );

            // Optimistic update: Update state WITHOUT saving to history
            // History is only saved when we actually call the API (in #saveStyleProperties)
            this.#uveStore.setPageAsset({ pageAsset: updatedInternalResponse });

            // Send updated response to iframe immediately for instant feedback
            // Get the updated custom response (computed will reflect the changes)
            const updatedCustomResponse = this.#uveStore.pageAsset()?.clientResponse;
            if (!updatedCustomResponse) {
                return;
            }

            this.#iframeMessenger.sendPageData(updatedCustomResponse);
        } catch (error) {
            console.error('Error updating iframe:', error);
        }
    }

    /**
     * Saves style properties to API with debounce
     * Saves current state to history before API call, so rollback can restore to this point
     */
    #saveStyleProperties(
        formValues: Record<string, unknown>,
        activeContentlet: ActionPayload | null,
        isTraditionalPage = false
    ): void {
        if (!activeContentlet) {
            return;
        }

        // Filter out null and undefined values before sending to API
        const filteredFormValues = filterFormValues(formValues);

        // Don't make API call if there are no values to save
        if (Object.keys(filteredFormValues).length === 0) {
            return;
        }

        // Save current state to history BEFORE making the API call
        // This ensures that if the API call fails, we can rollback to this exact state
        this.#uveStore.addCurrentPageToHistory();

        if (isTraditionalPage) {
            this.#uveStore.setUveStatus(UVE_STATUS.LOADING);
        }

        // Use the store's saveStyleEditor method which handles API call and rollback on failure
        // Subscribe to handle success/error and show toast notifications
        this.#uveStore
            .saveStyleEditor({
                containerIdentifier: activeContentlet.container.identifier,
                contentletIdentifier: activeContentlet.contentlet.identifier,
                styleProperties: filteredFormValues,
                pageId: activeContentlet.pageId,
                containerUUID: activeContentlet.container.uuid
            })
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: () => {
                    if (isTraditionalPage) {
                        this.#iframeMessenger.reloadPage();
                        this.#uveStore.setUveStatus(UVE_STATUS.LOADED);
                    }

                    // Success toast - style properties saved successfully
                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('message.content.saved'),
                        detail: this.#dotMessageService.get(
                            'message.content.note.already.published'
                        ),
                        life: 2000
                    });
                },
                error: (error) => {
                    // Restore form values from rolled-back state
                    // Rollback already happened synchronously in store's error handler,
                    // so we can restore the form immediately
                    this.#restoreFormFromRollback();

                    if (isTraditionalPage) {
                        this.#uveStore.setUveStatus(UVE_STATUS.LOADED);
                    }

                    // Error toast - rollback already handled in store
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get(
                            'editpage.content.update.contentlet.error'
                        ),
                        detail: error?.message || '',
                        life: 2000
                    });
                }
            });
    }
}
