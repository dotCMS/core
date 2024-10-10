import { animate, style, transition, trigger } from '@angular/animations';
import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit,
    output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { resolutionValue } from './utils';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';
import {
    CALENDAR_FIELD_TYPES,
    CONTENT_SEARCH_ROUTE,
    FLATTENED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FILTERED_TYPES } from '../../models/dot-edit-content-form.enum';
import { EditContentForm } from '../../models/dot-edit-content-form.interface';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import {
    getFinalCastedValue,
    isFilteredType,
    transformLayoutToTabs
} from '../../utils/functions.util';
import { DotEditContentAsideComponent } from '../dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

/**
 * Transforms the form data by filtering out specific field types and organizing the content into tabs.
 *
 * @param {EditContentForm} formData - The original form data to be transformed.
 * @param {DotMessageService} dotMessageService - Service used for internationalization.
 * @returns {EditContentForm} The transformed form data with filtered fields and organized tabs.
 */

const transformFormDataFn = (
    formData: EditContentForm,
    dotMessageService: DotMessageService
): EditContentForm => {
    if (!formData) {
        return { tabs: [] } as EditContentForm;
    }

    const tabs = transformLayoutToTabs(
        dotMessageService.get('Content'),
        formData.contentType.layout
    );

    const filteredTabs = tabs.map((tab) => ({
        ...tab,
        layout: tab.layout.map((row) => ({
            ...row,
            columns: row.columns.map((column) => ({
                ...column,
                fields: column.fields.filter((field) => !isFilteredType(field))
            }))
        }))
    }));

    return {
        ...formData,
        tabs: filteredTabs
    };
};

@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    templateUrl: './dot-edit-content-form.component.html',
    styleUrls: ['./dot-edit-content-form.component.scss'],
    imports: [
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        DotMessagePipe,
        DotEditContentAsideComponent,
        TabViewModule,
        DotWorkflowActionsComponent,
        TabViewInsertDirective,
        NgTemplateOutlet
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('fadeIn', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate('250ms ease-in', style({ opacity: 1 }))
            ])
        ])
    ]
})
export class DotEditContentFormComponent implements OnInit {
    readonly $store = inject(DotEditContentStore);
    readonly #router = inject(Router);
    readonly #destroyRef = inject(DestroyRef);
    readonly #fb = inject(FormBuilder);
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * This property is required and expects an object of type EditContentForm.
     * It is used to populate the form with initial data and structure.
     *
     * @type {EditContentForm}
     * @memberof DotEditContentFormComponent
     */
    $formData = input.required<EditContentForm, EditContentForm>({
        alias: 'formData',
        transform: (value) => transformFormDataFn(value, this.#dotMessageService)
    });

    /**
     * Output event emitter that informs when the form has changed.
     * Emits an object of type Record<string, string> containing the updated form values.
     *
     * @memberof DotEditContentFormComponent
     */
    changeValue = output<Record<string, string>>();

    /**
     * Computed signal that contains the type of the content type from $formData
     */
    $contentTypeName = computed(() => {
        const { contentType } = this.$formData();

        return contentType.name;
    });

    form!: FormGroup;

    readonly fieldTypes = FIELD_TYPES;
    readonly filteredTypes = FILTERED_TYPES;

    ngOnInit() {
        if (this.$formData()) {
            this.initializeForm();
            this.initializeFormListenter();
        }
    }

    /**
     * Initializes a listener for form value changes.
     * When the form value changes, it calls the onFormChange method with the new value.
     * The listener is automatically unsubscribed when the component is destroyed.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private initializeFormListenter() {
        this.form.valueChanges.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((value) => {
            const processedValue = this.processFormValue(value);
            this.changeValue.emit(processedValue);
        });
    }

    /**
     /**
     * Emits the form value through the `formSubmit` event.
     *
     * @param {*} value
     * @memberof DotEditContentFormComponent
     */
    onFormChange(value) {
        this.$formData().contentType.fields.forEach(({ variable, fieldType }) => {
            if (FLATTENED_FIELD_TYPES.includes(fieldType as FIELD_TYPES)) {
                value[variable] = value[variable]?.join(',');
            }

            if (CALENDAR_FIELD_TYPES.includes(fieldType as FIELD_TYPES)) {
                value[variable] = value[variable]
                    ?.toISOString()
                    .replace(/T|\.\d{3}Z/g, (match: string) => (match === 'T' ? ' ' : '')); // To remove the T and .000Z from the date)
            }
        });

        this.changeValue.emit(value);
    }

    /**
     * Processes the form value, applying specific transformations for certain field types.
     *
     * @private
     * @param {Record<string, any>} value The raw form value
     * @returns {Record<string, string>} The processed form value
     * @memberof DotEditContentFormComponent
     */
    private processFormValue(
        value: Record<string, string | string[] | Date | null | undefined>
    ): Record<string, string> {
        return Object.fromEntries(
            this.$formData().contentType.fields.map(({ variable, fieldType }) => {
                let fieldValue = value[variable];

                if (
                    Array.isArray(fieldValue) &&
                    FLATTENED_FIELD_TYPES.includes(fieldType as FIELD_TYPES)
                ) {
                    fieldValue = fieldValue.join(',');
                } else if (
                    fieldValue instanceof Date &&
                    CALENDAR_FIELD_TYPES.includes(fieldType as FIELD_TYPES)
                ) {
                    fieldValue = fieldValue
                        .toISOString()
                        .replace(/T|\.\d{3}Z/g, (match) => (match === 'T' ? ' ' : ''));
                }

                return [variable, fieldValue?.toString() ?? ''];
            })
        );
    }

    /**
     * Initializes the form by creating form controls for each field in the content type.
     * Skips filtered types.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private initializeForm() {
        this.form = this.#fb.group({});
        this.$formData().contentType.fields.forEach((field) => {
            if (!isFilteredType(field)) {
                const control = this.createFormControl(field);
                this.form.addControl(field.variable, control);
            }
        });
    }

    /**
     * Creates a form control for a given field.
     * Sets the initial value and validators for the control.
     *
     * @private
     * @param {DotCMSContentTypeField} field The field to create a control for
     * @returns {FormControl} The created form control
     * @memberof DotEditContentFormComponent
     */
    private createFormControl(field: DotCMSContentTypeField) {
        const initialValue = this.getInitialFieldValue(field, this.$formData().contentlet);
        const validators = this.getFieldValidators(field);

        return this.#fb.control({ value: initialValue, disabled: field.readOnly }, { validators });
    }

    /**
     * Retrieves the initial value for a given field based on the contentlet data.
     *
     * This method uses a resolution function specific to the field type to extract
     * the initial value from the contentlet. If no resolution function is found for
     * the field type, it logs a warning and returns null. The resolved value is then
     * cast to the appropriate type using getFinalCastedValue.
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field for which to get the initial value.
     * @param {DotCMSContentlet} contentlet - The contentlet object containing the field data.
     * @returns {any} The initial value for the field, or null if no value could be resolved.
     * @memberof DotEditContentFormComponent
     */
    private getInitialFieldValue(
        field: DotCMSContentTypeField,
        contentlet: DotCMSContentlet
    ): unknown {
        const resolutionFn = resolutionValue[field.fieldType as FIELD_TYPES];
        if (!resolutionFn) {
            console.warn(`No resolution function found for field type: ${field.fieldType}`);

            return null;
        }

        const value = resolutionFn(contentlet, field);

        return getFinalCastedValue(value, field) ?? null;
    }

    /**
     * Generates an array of validators for a given content type field.
     *
     * This method creates validators based on the field's properties:
     * - If the field is required, it adds a required validator.
     * - If the field has a regex check, it adds a pattern validator.
     *
     * If an invalid regex is provided, it logs an error but does not throw an exception.
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field for which to generate validators.
     * @returns {ValidatorFn[]} An array of Angular validator functions for the field.
     * @memberof DotEditContentFormComponent
     */
    private getFieldValidators(field: DotCMSContentTypeField): ValidatorFn[] {
        const validators: ValidatorFn[] = [];

        if (field.required) {
            validators.push(Validators.required);
        }

        if (field.regexCheck) {
            try {
                const regex = new RegExp(field.regexCheck);
                validators.push(Validators.pattern(regex));
            } catch (e) {
                console.error('Invalid regex', e);
            }
        }

        return validators;
    }

    /**
     * Fire the workflow action.
     *
     * @param {DotCMSWorkflowAction} action
     * @memberof EditContentLayoutComponent
     */
    fireWorkflowAction({ actionId, inode, contentType }: DotWorkflowActionParams): void {
        this.$store.fireWorkflowAction({
            actionId,
            inode,
            data: {
                contentlet: {
                    ...this.form.value,
                    contentType
                }
            }
        });
    }

    /**
     * Navigates back to the content listing page.
     *
     * This method uses the Angular Router service to navigate
     * to the content listing page with a filter based on the current content type.
     *
     * @memberof DotEditContentFormComponent
     */
    goBack(): void {
        const filter = this.$contentTypeName();
        this.#router.navigate([CONTENT_SEARCH_ROUTE], {
            queryParams: { filter }
        });
    }
}
