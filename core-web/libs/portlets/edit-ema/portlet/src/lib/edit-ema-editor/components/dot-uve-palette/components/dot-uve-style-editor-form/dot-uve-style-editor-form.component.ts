import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, OnChanges, SimpleChanges, inject, computed, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, ReactiveFormsModule, AbstractControl } from '@angular/forms';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';

import {
    StyleEditorFormSchema,
    StyleEditorSectionSchema,
    StyleEditorFieldSchema,
    StyleEditorRadioOptionObject,
    StyleEditorCheckboxDefaultValue
} from '@dotcms/uve';

interface ProcessedField extends StyleEditorFieldSchema {
    dropdownOptions?: StyleEditorRadioOptionObject[];
    checkboxOptions?: StyleEditorRadioOptionObject[];
    checkboxControls?: Map<string, FormControl>;
    radioOptions?: StyleEditorRadioOptionObject[];
    columns?: number;
    hasRadioImage?: boolean;
}

interface ProcessedSection extends StyleEditorSectionSchema {
    fields: ProcessedField[];
}

@Component({
    selector: 'dot-uve-style-editor-form',
    templateUrl: './dot-uve-style-editor-form.component.html',
    styleUrls: ['./dot-uve-style-editor-form.component.scss'],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        AccordionModule,
        DropdownModule,
        CheckboxModule,
        RadioButtonModule,
        InputTextModule,
        ButtonModule
    ],
    standalone: true
})
export class DotUveStyleEditorFormComponent implements OnInit, OnChanges {
    @Input() schema!: StyleEditorFormSchema;

    form!: FormGroup;
    private readonly processedSections = signal<ProcessedSection[]>([]);
    readonly sections = computed(() => this.processedSections());

    readonly #fb = inject(FormBuilder);

    ngOnInit(): void {
        if (this.schema) {
            this.buildForm();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['schema'] && !changes['schema'].firstChange && this.schema) {
            this.buildForm();
        }
    }

    private buildForm(): void {
        const formControls: Record<string, AbstractControl> = {};
        const processedSections: ProcessedSection[] = [];

        this.schema.sections.forEach((section: StyleEditorSectionSchema) => {
            const processedFields: ProcessedField[] = [];

            section.fields.forEach((field: StyleEditorFieldSchema) => {
                const fieldKey = field.id;
                const config = field.config;
                const processedField: ProcessedField = { ...field };

                switch (field.type) {
                    case 'dropdown': {
                        const options = config?.options || [];
                        processedField.dropdownOptions = options;
                        formControls[fieldKey] = this.#fb.control(
                            this.getDropdownDefaultValue(config)
                        );
                        break;
                    }

                    case 'checkboxGroup': {
                        const options = config?.options || [];
                        const checkboxDefaults = this.getCheckboxGroupDefaultValue(config);
                        const checkboxGroupControls: Record<string, FormControl> = {};
                        const checkboxControlsMap = new Map<string, FormControl>();

                        options.forEach((option) => {
                            const control = new FormControl(
                                checkboxDefaults[option.value] || false
                            );
                            checkboxGroupControls[option.value] = control;
                            checkboxControlsMap.set(option.value, control);
                        });

                        processedField.checkboxOptions = options;
                        processedField.checkboxControls = checkboxControlsMap;
                        formControls[fieldKey] = this.#fb.group(checkboxGroupControls);
                        break;
                    }

                    case 'radio': {
                        const options = config?.options || [];
                        processedField.radioOptions = options;
                        processedField.columns = config?.columns || 1;
                        processedField.hasRadioImage = options[0]?.imageURL !== undefined;
                        formControls[fieldKey] = this.#fb.control(
                            this.getRadioDefaultValue(config)
                        );
                        break;
                    }

                    case 'input':
                        formControls[fieldKey] = this.#fb.control(
                            this.getInputDefaultValue(config)
                        );
                        break;

                    default:
                        formControls[fieldKey] = this.#fb.control('');
                        break;
                }

                processedFields.push(processedField);
            });

            processedSections.push({
                ...section,
                fields: processedFields
            });
        });

        this.form = this.#fb.group(formControls);
        this.processedSections.set(processedSections);
    }

    private getDropdownDefaultValue(config: StyleEditorFieldSchema['config']): string {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue;
        }
        return config?.options?.[0]?.value || '';
    }

    private getCheckboxGroupDefaultValue(config: StyleEditorFieldSchema['config']): StyleEditorCheckboxDefaultValue {
        if (this.isCheckboxDefaultValue(config?.defaultValue)) {
            return config.defaultValue;
        }
        return {};
    }

    private getRadioDefaultValue(config: StyleEditorFieldSchema['config']): string {
        if (typeof config?.defaultValue === 'string') {
            return config.defaultValue;
        }
        return config?.options?.[0]?.value || '';
    }

    private getInputDefaultValue(config: StyleEditorFieldSchema['config']): string | number {
        if (typeof config?.defaultValue === 'string' || typeof config?.defaultValue === 'number') {
            return config.defaultValue;
        }
        return '';
    }

    private isCheckboxDefaultValue(value: unknown): value is StyleEditorCheckboxDefaultValue {
        return (
            typeof value === 'object' &&
            value !== null &&
            !Array.isArray(value) &&
            Object.values(value).every((v) => typeof v === 'boolean')
        );
    }


    printFormValues(): void {
        // Form values can be accessed via this.form.value
        console.log('Form Values:', this.form.value); // eslint-disable-line no-console
    }
}
