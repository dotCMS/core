import { CommonModule } from '@angular/common';
import {
    Component,
    input,
    inject,
    computed,
    signal,
    effect,
    DestroyRef,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { debounceTime, distinctUntilChanged, share, tap } from 'rxjs/operators';

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
    readonly $previousIndex = signal(-1);

    readonly #rollbackDetectionEffect = effect(() => {
        const currentIndex = this.#uveStore.currentIndex();
        const previousIndex = this.$previousIndex();

        // Detect rollback: index decreased AND we can undo (meaning undo() was called)
        // This ensures we only restore on actual rollbacks, not on addHistory() operations
        if (previousIndex >= 0 && currentIndex < previousIndex) {
            untracked(() => {
                this.#restoreFormFromRollback();
            });
        }
        this.$previousIndex.set(currentIndex);
    });

    $reloadSchemaEffect = effect(() => {
        // This allow to preserve the current value on the form when the schema is reloaded.
        // TODO: Remove untracked when we have the styleProperties in PageAPI response, also ensure that the form is rebuilt correctly.
        const schema = untracked(() => this.$schema());
        if (schema) {
            this.#buildForm(schema);
            this.#listenToFormChanges();
        }
    });

    /**
     * Builds a form from the schema using the form builder service
     */
    #buildForm(schema: StyleEditorFormSchema): void {
        const form = this.#formBuilder.buildForm(schema);
        this.#form.set(form);
    }

    /**
     * Restores form values from the rolled-back graphqlResponse state.
     * Used when rollback occurs to sync form with restored state.
     */
    #restoreFormFromRollback(): void {
        const form = this.#form();
        const activeContentlet = this.#uveStore.activeContentlet();

        if (!form || !activeContentlet) {
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

            if (styleProperties) {
                // Update form values without triggering valueChanges
                // Use patchValue with emitEvent: false to prevent triggering form changes
                form.patchValue(styleProperties, { emitEvent: false });
            }
        } catch (error) {
            console.error('Error restoring form from rollback:', error);
        }
    }

    /**
     * Listens to form changes and handles:
     * 1. Immediate updates to iframe (no debounce)
     * 2. Debounced API calls to save style properties
     */
    #listenToFormChanges(): void {
        const form = this.#form();
        if (!form) {
            return;
        }

        // Share the valueChanges observable to avoid multiple subscriptions
        const formValueChanges$ = form.valueChanges.pipe(
            share(),
            takeUntilDestroyed(this.#destroyRef)
        );

        formValueChanges$
            .pipe(
                distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
                tap((formValues) => this.#updateIframeImmediately(formValues)),
                debounceTime(STYLE_EDITOR_DEBOUNCE_TIME)
            )
            .subscribe((formValues: Record<string, unknown>) =>
                this.#saveStyleProperties(formValues)
            );
    }

    /**
     * Immediately updates the iframe with new form values (no debounce)
     * Uses optimistic updates WITHOUT saving to history (history is saved only on API calls)
     */
    #updateIframeImmediately(formValues: Record<string, unknown>): void {
        const activeContentlet = this.#uveStore.activeContentlet();

        if (!activeContentlet) {
            return;
        }

        try {
            // Get the internal graphqlResponse for optimistic update
            const internalGraphqlResponse = this.#uveStore.graphqlResponse();
            if (!internalGraphqlResponse) {
                return;
            }

            // Update the internal response (mutates the pageAsset in place)
            // Since $customGraphqlResponse is computed from graphqlResponse(),
            // updating the internal response will automatically reflect in the computed
            const updatedInternalResponse = updateStylePropertiesInGraphQL(
                internalGraphqlResponse,
                activeContentlet,
                formValues
            );

            // Optimistic update: Update state WITHOUT saving to history
            // History is only saved when we actually call the API (in #saveStylePropertiesToApi)
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
        const activeContentlet = this.#uveStore.activeContentlet();

        if (!activeContentlet) {
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
                styleProperties: formValues,
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
