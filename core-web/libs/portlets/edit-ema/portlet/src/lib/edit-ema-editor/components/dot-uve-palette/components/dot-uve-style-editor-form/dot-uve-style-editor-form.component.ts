import { Component, input, inject, computed, signal, effect, DestroyRef, untracked } from '@angular/core';
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
    
    $reloadSchemaEffect = effect(() => {
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
        graphqlResponse: DotCMSPageAsset | {
            grapql?: {
                query: string;
                variables: Record<string, string>;
            };
            pageAsset: DotCMSPageAsset;
            content?: Record<string, unknown>;
        },
        payload: ActionPayload,
        formValues: Record<string, unknown>
    ): DotCMSPageAsset | {
        grapql?: {
            query: string;
            variables: Record<string, string>;
        };
        pageAsset: DotCMSPageAsset;
        content?: Record<string, unknown>;
    } {
        // Handle both types: DotCMSPageAsset directly or wrapped in pageAsset property
        const pageAsset = 'pageAsset' in graphqlResponse 
            ? graphqlResponse.pageAsset 
            : graphqlResponse;
        
        const containerId = payload.container.identifier;
        const contentletId = payload.contentlet.identifier;
        const uuid = payload.container.uuid;
        
        const container = pageAsset.containers[containerId];
        
        if (!container) {
            console.error(`Container with id ${containerId} not found`);
            return graphqlResponse;
        }
        
        const contentlets = container.contentlets[`uuid-${uuid}`]

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
        formValueChanges$.pipe(
            distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
        ).subscribe((formValues) => {
            this.#updateIframeImmediately(formValues);
        });

        // Debounced subscription: Save to API
        formValueChanges$.pipe(
            debounceTime(1000),
            distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
        ).subscribe((formValues) => {
            this.#saveStylePropertiesToApi(formValues);
        });
    }

    /**
     * Immediately updates the iframe with new form values (no debounce)
     */
    #updateIframeImmediately(formValues: Record<string, unknown>): void {
        const activeContentlet = this.#uveStore.activeContentlet();
        const graphqlResponse = this.#uveStore.$customGraphqlResponse();
        
        if (!activeContentlet || !graphqlResponse) {
            return;
        }

        try {
            const updatedResponse = this.#updateGraphQLResponse(
                graphqlResponse,
                activeContentlet,
                formValues
            );
            
            // Send updated response to iframe immediately for instant feedback
            this.#iframeMessenger.sendPageData(updatedResponse);
        } catch (error) {
            console.error('Error updating iframe:', error);
        }
    }

    /**
     * Saves style properties to API with debounce
     */
    async #saveStylePropertiesToApi(formValues: Record<string, unknown>): Promise<void> {
        const activeContentlet = this.#uveStore.activeContentlet();
        
        if (!activeContentlet) {
            return;
        }

        const payload = activeContentlet;
        
        try {
            await this.#dotPageApiService.saveStyleProperties({
                containerIdentifier: payload.container.identifier,
                contentledIdentifier: payload.contentlet.identifier,
                styleProperties: formValues,
                pageId: payload.pageId,
                containerUUID: payload.container.uuid
            }).toPromise();
        } catch (error) {
            console.error('Error saving style properties:', error);
            // TODO: Add error toast notification
        }
    }
}
