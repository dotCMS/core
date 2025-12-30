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
import {
    FormBuilder,
    FormGroup,
    FormControl,
    ReactiveFormsModule,
    AbstractControl
} from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';

import { debounceTime, distinctUntilChanged, share } from 'rxjs/operators';

import { DotCMSBasicContentlet, DotCMSPageAsset } from '@dotcms/types';
import {
    StyleEditorFormSchema,
    StyleEditorSectionSchema,
    StyleEditorFieldSchema,
    StyleEditorCheckboxDefaultValue
} from '@dotcms/uve';

import { UveStyleEditorFieldCheckboxGroupComponent } from './components/uve-style-editor-field-checkbox-group/uve-style-editor-field-checkbox-group.component';
import { UveStyleEditorFieldDropdownComponent } from './components/uve-style-editor-field-dropdown/uve-style-editor-field-dropdown.component';
import { UveStyleEditorFieldInputComponent } from './components/uve-style-editor-field-input/uve-style-editor-field-input.component';
import { UveStyleEditorFieldRadioComponent } from './components/uve-style-editor-field-radio/uve-style-editor-field-radio.component';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';
import { UveIframeMessengerService } from '../../../../../services/uve-iframe-messenger.service';
import { STYLE_EDITOR_FIELD_TYPES } from '../../../../../shared/consts';
import { ActionPayload } from '../../../../../shared/models';
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

    readonly #fb = inject(FormBuilder);
    readonly #form = signal<FormGroup | null>(null);
    readonly #uveStore = inject(UVEStore);
    readonly #iframeMessenger = inject(UveIframeMessengerService);
    readonly #dotPageApiService = inject(DotPageApiService);
    readonly #destroyRef = inject(DestroyRef);

    $sections = computed(() => this.$schema().sections);
    $form = computed(() => this.#form());

    // TEST ONLY: Time machine state for visual debugging
    $timeMachineState = computed(() => ({
        historyLength: this.#uveStore.historyLength(),
        currentIndex: this.#uveStore.currentIndex(),
        canUndo: this.#uveStore.canUndo(),
        canRedo: this.#uveStore.canRedo(),
        isAtStart: this.#uveStore.isAtStart(),
        isAtEnd: this.#uveStore.isAtEnd(),
        haveHistory: this.#uveStore.haveHistory()
    }));

    readonly #previousIndex = signal(-1);

    readonly #rollbackDetectionEffect = effect(() => {
        const currentIndex = this.#uveStore.currentIndex();
        const previousIndex = this.#previousIndex();

        // Detect rollback: index decreased (moved backwards in history)
        if (previousIndex >= 0 && currentIndex < previousIndex) {
            this.#restoreFormFromRollback();
        }
        this.#previousIndex.set(currentIndex);
    });

    $reloadSchemaEffect = effect(() => {
        // Added this untracked ONLY while we dont have the styleProperties in PageAPI response.
        // This allow to preserve the current value on the form when the schema is reloaded.
        const schema = untracked(() => this.$schema());
        if (schema) {
            this.#buildForm(schema);
            this.#listenToFormChanges();
        }
    });

    readonly STYLE_EDITOR_FIELD_TYPES = STYLE_EDITOR_FIELD_TYPES;

    #buildForm(schema: StyleEditorFormSchema): void {
        const formControls: Record<string, AbstractControl> = {};

        schema.sections.forEach((section: StyleEditorSectionSchema) => {
            section.fields.forEach((field: StyleEditorFieldSchema) => {
                const fieldKey = field.id;
                const config = field.config;

                switch (field.type) {
                    case STYLE_EDITOR_FIELD_TYPES.DROPDOWN:
                        formControls[fieldKey] = this.#fb.control(
                            this.#getDropdownDefaultValue(config)
                        );
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.CHECKBOX_GROUP: {
                        const options = config?.options || [];
                        const checkboxDefaults = this.#getCheckboxGroupDefaultValue(config);
                        const checkboxGroupControls: Record<string, FormControl> = {};

                        options.forEach((option) => {
                            checkboxGroupControls[option.value] = new FormControl(
                                checkboxDefaults[option.value] || false
                            );
                        });

                        formControls[fieldKey] = this.#fb.group(checkboxGroupControls);
                        break;
                    }

                    case STYLE_EDITOR_FIELD_TYPES.RADIO:
                        formControls[fieldKey] = this.#fb.control(
                            this.#getRadioDefaultValue(config)
                        );
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.INPUT:
                        formControls[fieldKey] = this.#fb.control(
                            this.#getInputDefaultValue(config)
                        );
                        break;

                    default:
                        formControls[fieldKey] = this.#fb.control('');
                        break;
                }
            });
        });

        this.#form.set(this.#fb.group(formControls));
    }

    #getDropdownDefaultValue(config: StyleEditorFieldSchema['config']): string {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue.trim();
        }

        return null;
    }

    #getCheckboxGroupDefaultValue(
        config: StyleEditorFieldSchema['config']
    ): StyleEditorCheckboxDefaultValue {
        if (this.#isCheckboxDefaultValue(config?.defaultValue)) {
            return config.defaultValue;
        }
        return {};
    }

    #getRadioDefaultValue(config: StyleEditorFieldSchema['config']): string {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue;
        }
        return config?.options?.[0]?.value || '';
    }

    #getInputDefaultValue(config: StyleEditorFieldSchema['config']): string | number {
        if (typeof config?.defaultValue === 'string' || typeof config?.defaultValue === 'number') {
            return config.defaultValue;
        }
        return '';
    }

    #isCheckboxDefaultValue(value: unknown): value is StyleEditorCheckboxDefaultValue {
        return (
            typeof value === 'object' &&
            value !== null &&
            !Array.isArray(value) &&
            Object.values(value).every((v) => typeof v === 'boolean')
        );
    }

    /**
     * Gets the current form values
     *
     * @returns The raw form values or null if form is not available
     */
    #getFormValues(): Record<string, unknown> | null {
        return this.#form() ? this.#form().getRawValue() : null;
    }

    /**
     * Updates the graphqlResponse with form values.
     * Similar to the test() method logic but uses the payload to find the correct contentlet.
     *
     * @param graphqlResponse - The current graphql response
     * @param payload - The action payload containing container and contentlet info
     * @param formValues - The form values to apply to the contentlet
     * @returns The updated graphql response
     */
    #updateGraphQLResponse(
        graphqlResponse:
            | DotCMSPageAsset
            | {
                  grapql?: {
                      query: string;
                      variables: Record<string, string>;
                  };
                  pageAsset: DotCMSPageAsset;
                  content?: Record<string, unknown>;
              },
        payload: ActionPayload,
        formValues: Record<string, unknown>
    ):
        | DotCMSPageAsset
        | {
              grapql?: {
                  query: string;
                  variables: Record<string, string>;
              };
              pageAsset: DotCMSPageAsset;
              content?: Record<string, unknown>;
          } {
        // Handle both types: DotCMSPageAsset directly or wrapped in pageAsset property
        const pageAsset =
            'pageAsset' in graphqlResponse ? graphqlResponse.pageAsset : graphqlResponse;

        const containerId = payload.container.identifier;
        const contentletId = payload.contentlet.identifier;
        const uuid = payload.container.uuid;

        const container = pageAsset.containers[containerId];

        if (!container) {
            console.error(`Container with id ${containerId} not found`);
            return graphqlResponse;
        }

        const contentlets = container.contentlets[`uuid-${uuid}`];

        if (!contentlets) {
            console.error(`Contentlet with uuid ${uuid} not found`);
            return graphqlResponse;
        }

        contentlets.forEach((contentlet: DotCMSBasicContentlet) => {
            if (contentlet?.identifier === contentletId) {
                contentlet.style_properties = formValues;
            }
        });

        return graphqlResponse;
    }

    /**
     * Extracts style properties from graphqlResponse for a specific contentlet.
     * Reverse operation of #updateGraphQLResponse - used to restore form values on rollback.
     *
     * @param graphqlResponse - The graphql response to extract from
     * @param payload - The action payload containing container and contentlet info
     * @returns The style properties object or null if not found
     */
    #extractStylePropertiesFromGraphQLResponse(
        graphqlResponse:
            | DotCMSPageAsset
            | {
                  grapql?: {
                      query: string;
                      variables: Record<string, string>;
                  };
                  pageAsset: DotCMSPageAsset;
                  content?: Record<string, unknown>;
              },
        payload: ActionPayload
    ): Record<string, unknown> | null {
        // Handle both types: DotCMSPageAsset directly or wrapped in pageAsset property
        const pageAsset =
            'pageAsset' in graphqlResponse ? graphqlResponse.pageAsset : graphqlResponse;

        const containerId = payload.container.identifier;
        const contentletId = payload.contentlet.identifier;
        const uuid = payload.container.uuid;

        const container = pageAsset.containers[containerId];

        if (!container) {
            return null;
        }

        const contentlets = container.contentlets[`uuid-${uuid}`];

        if (!contentlets) {
            return null;
        }

        const contentlet = contentlets.find(
            (c: DotCMSBasicContentlet) => c?.identifier === contentletId
        );

        return contentlet?.style_properties || null;
    }

    /**
     * Restores form values from the rolled-back graphqlResponse state.
     * TEST ONLY: Used when rollback occurs to sync form with restored state.
     */
    #restoreFormFromRollback(): void {
        const form = this.#form();
        const activeContentlet = this.#uveStore.activeContentlet();
        const graphqlResponse = this.#uveStore.$customGraphqlResponse();

        if (!form || !activeContentlet || !graphqlResponse) {
            return;
        }

        try {
            // Extract style properties from the rolled-back state
            const styleProperties = this.#extractStylePropertiesFromGraphQLResponse(
                graphqlResponse,
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

        // Immediate subscription: Update iframe without debounce
        formValueChanges$
            .pipe(
                distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
            )
            .subscribe((formValues) => {
                this.#updateIframeImmediately(formValues);
            });

        // Debounced subscription: Save to API
        formValueChanges$
            .pipe(
                debounceTime(3000),
                distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
            )
            .subscribe((formValues) => {
                this.#saveStylePropertiesToApi(formValues);
            });
    }

    /**
     * Immediately updates the iframe with new form values (no debounce)
     * Uses optimistic updates WITHOUT saving to history (history is saved only on API calls)
     */
    #updateIframeImmediately(formValues: Record<string, unknown>): void {
        const activeContentlet = this.#uveStore.activeContentlet();
        const customGraphqlResponse = this.#uveStore.$customGraphqlResponse();

        if (!activeContentlet || !customGraphqlResponse) {
            return;
        }

        try {
            // Get the internal graphqlResponse (always wrapped format) for optimistic update
            const internalGraphqlResponse = this.#uveStore.graphqlResponse();
            if (!internalGraphqlResponse) {
                return;
            }

            // Update the internal response (mutates the pageAsset in place)
            // Since $customGraphqlResponse is computed from graphqlResponse(),
            // updating the internal response will automatically reflect in the computed
            const updatedInternalResponse = this.#updateGraphQLResponse(
                internalGraphqlResponse,
                activeContentlet,
                formValues
            ) as typeof internalGraphqlResponse;

            // Optimistic update: Update state WITHOUT saving to history
            // History is only saved when we actually call the API (in #saveStylePropertiesToApi)
            this.#uveStore.setGraphqlResponse(updatedInternalResponse);

            // Send updated response to iframe immediately for instant feedback
            // Get the updated custom response (computed will reflect the changes)
            const updatedCustomResponse = this.#uveStore.$customGraphqlResponse();
            if (updatedCustomResponse) {
                this.#iframeMessenger.sendPageData(updatedCustomResponse);
            }
        } catch (error) {
            console.error('Error updating iframe:', error);
        }
    }

    /**
     * Saves style properties to API with debounce
     * Saves current state to history before API call, so rollback can restore to this point
     */
    #saveStylePropertiesToApi(formValues: Record<string, unknown>): void {
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

        const payload = activeContentlet;

        // Use the store's saveStyleEditor method which handles API call and rollback on failure
        this.#uveStore.saveStyleEditor({
            containerIdentifier: payload.container.identifier,
            contentledIdentifier: payload.contentlet.identifier,
            styleProperties: formValues,
            pageId: payload.pageId,
            containerUUID: payload.container.uuid
        });
    }
}
