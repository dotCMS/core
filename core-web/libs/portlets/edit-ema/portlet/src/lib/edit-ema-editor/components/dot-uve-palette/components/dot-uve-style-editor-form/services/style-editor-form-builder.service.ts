import { Injectable, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup } from '@angular/forms';

import {
    StyleEditorCheckboxDefaultValue,
    StyleEditorFieldSchema,
    StyleEditorFormSchema,
    StyleEditorSectionSchema
} from '@dotcms/uve';

import { STYLE_EDITOR_FIELD_TYPES } from '../../../../../../shared/consts';
import { StyleEditorProperties } from '../../../../../../shared/models';

/**
 * Service responsible for building reactive forms from style editor schemas.
 * Handles form control creation using initial values from styleProperties or empty values.
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
     * @param initialValues - Optional initial values to populate the form with (from styleProperties)
     * @returns A FormGroup with controls for all fields in the schema
     */
    buildForm(schema: StyleEditorFormSchema, initialValues?: StyleEditorProperties): FormGroup {
        const formControls: Record<string, AbstractControl> = {};

        schema.sections.forEach((section: StyleEditorSectionSchema) => {
            section.fields.forEach((field: StyleEditorFieldSchema) => {
                const fieldKey = field.id;
                const initialValue = initialValues?.[fieldKey];

                switch (field.type) {
                    case STYLE_EDITOR_FIELD_TYPES.DROPDOWN:
                        formControls[fieldKey] = this.#fb.control(
                            initialValue !== undefined ? initialValue : null
                        );
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.CHECKBOX_GROUP: {
                        const options = field.config?.options || [];
                        const checkboxInitialValues =
                            (initialValue as StyleEditorCheckboxDefaultValue | undefined) || {};
                        const checkboxGroupControls: Record<string, FormControl> = {};

                        options.forEach((option) => {
                            checkboxGroupControls[option.value] = new FormControl(
                                checkboxInitialValues[option.value] || false
                            );
                        });

                        formControls[fieldKey] = this.#fb.group(checkboxGroupControls);
                        break;
                    }

                    case STYLE_EDITOR_FIELD_TYPES.RADIO:
                        formControls[fieldKey] = this.#fb.control(
                            initialValue !== undefined ? initialValue : null
                        );
                        break;

                    case STYLE_EDITOR_FIELD_TYPES.INPUT: {
                        // Determine empty value based on inputType
                        // Number inputs use null, text inputs use empty string
                        const emptyValue = field.config?.inputType === 'number' ? null : '';
                        formControls[fieldKey] = this.#fb.control(
                            initialValue !== undefined ? initialValue : emptyValue
                        );
                        break;
                    }

                    default:
                        formControls[fieldKey] = this.#fb.control('');
                        break;
                }
            });
        });

        return this.#fb.group(formControls);
    }
}
