import { CommonModule } from '@angular/common';
import {
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    linkedSignal,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { UveStyleEditorFieldCheckboxGroupComponent } from './components/uve-style-editor-field-checkbox-group/uve-style-editor-field-checkbox-group.component';
import { UveStyleEditorFieldDropdownComponent } from './components/uve-style-editor-field-dropdown/uve-style-editor-field-dropdown.component';
import { UveStyleEditorFieldInputComponent } from './components/uve-style-editor-field-input/uve-style-editor-field-input.component';
import { UveStyleEditorFieldRadioComponent } from './components/uve-style-editor-field-radio/uve-style-editor-field-radio.component';
import { StyleEditorFormBuilderService } from './services/style-editor-form-builder.service';
import {
    extractStylePropertiesFromGraphQL,
    updateStylePropertiesInGraphQL
} from './utils/style-editor-graphql.utils';

import { UveIframeMessengerService } from '../../../../../services/iframe-messenger/uve-iframe-messenger.service';
import { STYLE_EDITOR_DEBOUNCE_TIME, STYLE_EDITOR_FIELD_TYPES } from '../../../../../shared/consts';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { filterFormValues } from '../../utils';

@Component({
    selector: 'dot-uve-style-editor-form',
    templateUrl: './dot-uve-style-editor-form.component.html',
    styleUrls: ['./dot-uve-style-editor-form.component.scss'],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        AccordionModule,
        ButtonModule,
        UveStyleEditorFieldInputComponent,
        UveStyleEditorFieldDropdownComponent,
        UveStyleEditorFieldCheckboxGroupComponent,
        UveStyleEditorFieldRadioComponent
    ]
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

    $sections = computed(() => this.$schema().sections);
    $form = computed(() => this.#form());

    readonly STYLE_EDITOR_FIELD_TYPES = STYLE_EDITOR_FIELD_TYPES;

    /**
     * Tracks rollback detection using linkedSignal.
     * Returns true when currentIndex decreases (undo operation detected).
     * The previous parameter contains both the source (currentIndex) and the computed value from last run.
     */
    readonly $isRollback = linkedSignal({
        source: this.#uveStore.currentIndex,
        computation: (currentIndex: number, previous?: { source: number; value: boolean }) => {
            // First run: no previous value, not a rollback
            if (!previous) {
                return false;
            }

            const previousIndex = previous.source;

            // Rollback detected: index decreased from a valid position
            return previousIndex >= 0 && currentIndex < previousIndex;
        }
    });

    /**
     * Computed property that returns an array of all section indices to keep all tabs open by default
     */
    $activeTabIndices = computed(() => {
        const sections = this.$sections();
        return sections.map((_, index) => index);
    });

    // readonly #rollbackDetectionEffect = effect(() => {
    //     const isRollback = this.$isRollback();

    //     // When rollback is detected, restore form from the rolled-back state
    //     if (isRollback) {
    //         untracked(() => {
    //             this.#restoreFormFromRollback();
    //         });
    //     }
    // });

    $reloadSchemaEffect = effect(() => {
        const schema = untracked(() => this.$schema());

        if (schema) {
            this.#buildForm(schema);
        }
    });

    constructor() {
        // Use switchMap to automatically cancel pending saves when form is rebuilt
        this.#listenToFormChanges();
    }

    /**
     * Builds a form from the schema using the form builder service
     */
    #buildForm(schema: StyleEditorFormSchema): void {
        const activeContentlet = this.#uveStore.editor().activeContentlet;

        // Get styleProperties directly from the contentlet payload (already in the postMessage)
        const initialValues = activeContentlet?.contentlet?.dotStyleProperties;

        const form = this.#formBuilder.buildForm(schema, initialValues);
        this.#form.set(form);
    }

    /**
     * Restores form values from the rolled-back graphqlResponse state.
     * Used when rollback occurs to sync form with restored state.
     *
     * This method rebuilds the entire form (rather than patching) to trigger
     * the switchMap in #listenToFormChanges, which automatically cancels any
     * pending debounced saves from the old form instance.
     */
    #restoreFormFromRollback(): void {
        const activeContentlet = this.#uveStore.editor.activeContentlet();
        const schema = this.$schema();

        if (!activeContentlet || !schema) {
            return;
        }

        try {
            // Use the internal graphqlResponse signal directly (it's already been rolled back)
            // This ensures we get the rolled-back state, not the computed wrapper
            const rolledBackGraphqlResponse = this.#uveStore.graphqlResponse();

            if (!rolledBackGraphqlResponse) {
                return;
            }

            // Extract style properties from the rolled-back state using utility function
            const styleProperties = extractStylePropertiesFromGraphQL(
                rolledBackGraphqlResponse,
                activeContentlet
            );

            // Rebuild the ENTIRE form with rolled-back values
            // This causes the #form signal to change, which triggers switchMap in #listenToFormChanges
            // to cancel the old subscription (including any pending debounced saves)
            const restoredForm = this.#formBuilder.buildForm(schema, styleProperties || undefined);
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
     * Uses switchMap to automatically cancel pending saves when the form is rebuilt
     * (e.g., during rollback restoration). This is a clean reactive approach that
     * eliminates the need for flags, timeouts, or manual subscription management.
     */
    #listenToFormChanges(): void {
        // Convert the form signal to an observable
        // When the form signal changes (rebuilt during rollback), switchMap cancels the old subscription
        toObservable(this.$form)
            .pipe(
                // Filter out null forms
                filter((form): form is FormGroup => form !== null),
                // Switch to the new form's valueChanges
                // This automatically unsubscribes from the previous form's valueChanges
                // and cancels any pending debounced saves
                switchMap((form) =>
                    form.valueChanges.pipe(
                        distinctUntilChanged(
                            (prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)
                        ),
                        tap((formValues) => this.#updateIframeImmediately(formValues)),
                        debounceTime(STYLE_EDITOR_DEBOUNCE_TIME)
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe((formValues: Record<string, unknown>) => {
                this.#saveStyleProperties(formValues);
            });
    }

    /**
     * Immediately updates the iframe with new form values (no debounce)
     * Uses optimistic updates WITHOUT saving to history (history is saved only on API calls)
     */
    #updateIframeImmediately(formValues: Record<string, unknown>): void {
        const activeContentlet = this.#uveStore.editor().activeContentlet;

        if (!activeContentlet) {
            return;
        }

        try {
            // Get the internal graphqlResponse for optimistic update
            const internalGraphqlResponse = this.#uveStore.graphqlResponse();
            if (!internalGraphqlResponse) {
                return;
            }

            // Deep clone the graphqlResponse before mutating to prevent affecting history entries
            // This ensures that mutations don't affect the stored state in history
            const clonedResponse = structuredClone(internalGraphqlResponse);

            // Update the cloned response (mutates the clone in place)
            const updatedInternalResponse = updateStylePropertiesInGraphQL(
                clonedResponse,
                activeContentlet,
                formValues
            );

            // Optimistic update: Update state WITHOUT saving to history
            // History is only saved when we actually call the API (in #saveStyleProperties)
            this.#uveStore.setGraphqlResponse(updatedInternalResponse);

            // Send updated response to iframe immediately for instant feedback
            // Get the updated custom response (computed will reflect the changes)
            const updatedCustomResponse = this.#uveStore.$customGraphqlResponse();
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
    #saveStyleProperties(formValues: Record<string, unknown>): void {
        const activeContentlet = this.#uveStore.editor().activeContentlet;

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
        const currentGraphqlResponse = this.#uveStore.graphqlResponse();
        if (currentGraphqlResponse) {
            this.#uveStore.addHistory(currentGraphqlResponse);
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
