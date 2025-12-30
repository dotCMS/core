import { STYLE_EDITOR_FIELD_TYPES } from 'libs/portlets/edit-ema/portlet/src/lib/shared/consts';

import { Injectable, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, AbstractControl } from '@angular/forms';

import {
    StyleEditorFormSchema,
    StyleEditorSectionSchema,
    StyleEditorFieldSchema,
    StyleEditorCheckboxDefaultValue
} from '@dotcms/uve';

/**
 * Service responsible for building reactive forms from style editor schemas.
 * Handles form control creation and default value extraction for different field types.
 */
@Injectable({
    providedIn: 'root'
})
export class StyleEditorFormBuilderService {
    readonly #fb = inject(FormBuilder);

    /**
     * Builds a FormGroup from a StyleEditorFormSchema
     *
     * @param schema - The style editor form schema
     * @returns A FormGroup with controls for all fields in the schema
     */
    buildForm(schema: StyleEditorFormSchema): FormGroup {
        const formControls: Record<string, AbstractControl> = {};

        schema.sections.forEach((section: StyleEditorSectionSchema) => {
            section.fields.forEach((field: StyleEditorFieldSchema) => {
                const fieldKey = field.id;
                const defaultValue = this.getDefaultValue(field);

                switch (field.type) {
                    case STYLE_EDITOR_FIELD_TYPES.DROPDOWN:
                        formControls[fieldKey] = this.#fb.control(defaultValue);
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.CHECKBOX_GROUP: {
                        const options = field.config?.options || [];
                        const checkboxDefaults = this.getCheckboxGroupDefaultValue(field.config);
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
                        formControls[fieldKey] = this.#fb.control(defaultValue);
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.INPUT:
                        formControls[fieldKey] = this.#fb.control(defaultValue);
                        break;

                    default:
                        formControls[fieldKey] = this.#fb.control('');
                        break;
                }
            });
        });

        return this.#fb.group(formControls);
    }

    /**
     * Gets the default value for a field based on its type and configuration
     */
    private getDefaultValue(field: StyleEditorFieldSchema): unknown {
        const config = field.config;

        switch (field.type) {
            case STYLE_EDITOR_FIELD_TYPES.DROPDOWN:
                return this.getDropdownDefaultValue(config);

            case STYLE_EDITOR_FIELD_TYPES.CHECKBOX_GROUP:
                return this.getCheckboxGroupDefaultValue(config);

            case STYLE_EDITOR_FIELD_TYPES.RADIO:
                return this.getRadioDefaultValue(config);

            case STYLE_EDITOR_FIELD_TYPES.INPUT:
                return this.getInputDefaultValue(config);

            default:
                return '';
        }
    }

    /**
     * Gets the default value for a dropdown field
     */
    private getDropdownDefaultValue(config: StyleEditorFieldSchema['config']): string | null {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue.trim();
        }
        return null;
    }

    /**
     * Gets the default value for a checkbox group field
     */
    private getCheckboxGroupDefaultValue(
        config: StyleEditorFieldSchema['config']
    ): StyleEditorCheckboxDefaultValue {
        if (this.isCheckboxDefaultValue(config?.defaultValue)) {
            return config.defaultValue;
        }
        return {};
    }

    /**
     * Gets the default value for a radio field
     */
    private getRadioDefaultValue(config: StyleEditorFieldSchema['config']): string {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue;
        }
        return config?.options?.[0]?.value || '';
    }

    /**
     * Gets the default value for an input field
     */
    private getInputDefaultValue(config: StyleEditorFieldSchema['config']): string | number {
        if (typeof config?.defaultValue === 'string' || typeof config?.defaultValue === 'number') {
            return config.defaultValue;
        }
        return '';
    }

    /**
     * Type guard to check if a value is a valid checkbox default value
     */
    private isCheckboxDefaultValue(value: unknown): value is StyleEditorCheckboxDefaultValue {
        return (
            typeof value === 'object' &&
            value !== null &&
            !Array.isArray(value) &&
            Object.values(value).every((v) => typeof v === 'boolean')
        );
    }
}
