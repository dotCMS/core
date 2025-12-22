import { Component, input, inject, computed, signal, effect } from '@angular/core';
import {
    FormBuilder,
    FormGroup,
    FormControl,
    ReactiveFormsModule,
    AbstractControl
} from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';

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

import { STYLE_EDITOR_FIELD_TYPES } from '../../../../../shared/consts';

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

    $sections = computed(() => this.$schema().sections);
    $form = computed(() => this.#form());

    $reloadSchemaEffect = effect(() => {
        const schema = this.$schema();
        if (schema) {
            this.#buildForm(schema);
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
        return config?.options?.[0]?.value || '';
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
}
