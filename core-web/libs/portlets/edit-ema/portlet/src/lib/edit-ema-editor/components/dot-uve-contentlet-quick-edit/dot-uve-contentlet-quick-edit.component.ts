import { Component, input, output, inject, effect, signal, computed } from '@angular/core';
import { FormGroup, FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { MultiSelectModule } from 'primeng/multiselect';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotCMSClazzes, DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContainerPayload } from '../../../shared/models';

/**
 * Pick only the fields needed for the quick-edit form from DotCMSContentTypeField.
 * Extends with options property for dropdown/checkbox/radio rendering.
 */
export type ContentletField = Pick<
    DotCMSContentTypeField,
    'name' | 'variable' | 'clazz' | 'required' | 'readOnly' | 'regexCheck' | 'dataType'
> & {
    options?: Array<{ label: string; value: string }>;
};

export interface ContentletEditData {
    container: ContainerPayload;
    contentlet: DotCMSContentlet;
    fields: ContentletField[];
}

/**
 * Presentational component for quick-editing contentlet form fields in the right sidebar.
 * NO store injection - receives all data via @Input, emits events via @Output.
 * Container controls visibility with @if directive.
 *
 * @example
 * ```html
 * @if ($rightSidebarOpen()) {
 *   <dot-uve-contentlet-quick-edit
 *     [data]="$contentletEditData()"
 *     [loading]="$isSubmitting()"
 *     (submit)="onFormSubmit($event)"
 *     (cancel)="onCancel()" />
 * }
 * ```
 */
@Component({
    selector: 'dot-uve-contentlet-quick-edit',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        CheckboxModule,
        DropdownModule,
        InputTextModule,
        InputTextareaModule,
        MultiSelectModule,
        RadioButtonModule
    ],
    templateUrl: './dot-uve-contentlet-quick-edit.component.html',
    styleUrl: './dot-uve-contentlet-quick-edit.component.scss'
})
export class DotUveContentletQuickEditComponent {
    private readonly fb = inject(FormBuilder);

    // Inputs (data down from parent container)
    data = input.required<ContentletEditData>({ alias: 'data' });
    loading = input<boolean>(false, { alias: 'loading' });

    // Outputs (events up to parent container)
    submit = output<Record<string, unknown>>();
    cancel = output<void>();

    // Internal form state
    private readonly contentletForm = signal<FormGroup | null>(null);
    protected readonly $contentletForm = computed(() => this.contentletForm());

    protected readonly DotCMSClazzes = DotCMSClazzes;

    constructor() {
        // Build form when data changes
        effect(() => {
            const { fields, contentlet } = this.data();

            if (!fields || fields.length === 0) {
                this.contentletForm.set(null);
                return;
            }

            this.buildForm(fields, contentlet);
        });
    }

    private buildForm(fields: ContentletField[], contentlet: DotCMSContentlet): void {
        const formControls: Record<string, AbstractControl> = {};

        // Add hidden inode field
        if (contentlet?.inode) {
            formControls['inode'] = this.fb.control(contentlet.inode);
        }

        fields.forEach((field) => {
            let fieldValue: string | string[] | boolean = contentlet?.[field.variable] ?? '';
            const validators = [];

            // Handle checkbox with multiple options - value should be an array
            if (field.clazz === DotCMSClazzes.CHECKBOX && field.options && field.options.length > 0) {
                // Convert string value to array if needed
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.split(',').map((v) => v.trim());
                } else if (!Array.isArray(fieldValue)) {
                    fieldValue = [];
                }
            }

            // Handle multi-select - value should be an array
            if (field.clazz === DotCMSClazzes.MULTI_SELECT) {
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.split(',').map((v) => v.trim());
                } else if (!Array.isArray(fieldValue)) {
                    fieldValue = [];
                }
            }

            if (field.required) {
                validators.push(Validators.required);
            }

            if (field.regexCheck) {
                try {
                    // Validate the regex pattern before using it
                    new RegExp(field.regexCheck);
                    validators.push(Validators.pattern(field.regexCheck));
                } catch (error) {
                    // Skip invalid regex patterns
                    console.warn(
                        `Invalid regex pattern for field ${field.variable}: ${field.regexCheck}`,
                        error
                    );
                }
            }

            formControls[field.variable] = this.fb.control(
                fieldValue,
                validators.length > 0 ? validators : null
            );

            if (field.readOnly) {
                formControls[field.variable].disable();
            }
        });

        this.contentletForm.set(this.fb.group(formControls));
    }

    protected handleSubmit(): void {
        const form = this.$contentletForm();
        if (form?.valid) {
            this.submit.emit(form.value);
        }
    }

    protected handleCancel(): void {
        this.cancel.emit();
    }
}
