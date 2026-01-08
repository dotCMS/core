import { Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
    computed,
    inject,
    input
} from '@angular/core';
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

import { takeUntil } from 'rxjs/operators';

import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    FeaturedFlags,
    NEW_RENDER_MODE_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { isEqual } from '@dotcms/utils';

import { FieldPropertyService } from '../service';

@Component({
    selector: 'dot-content-type-fields-properties-form',
    styleUrls: ['./content-type-fields-properties-form.component.scss'],
    templateUrl: './content-type-fields-properties-form.component.html',
    standalone: false
})
export class ContentTypeFieldsPropertiesFormComponent implements OnChanges, OnInit, OnDestroy {
    /** Form builder instance for creating reactive forms */
    private fb = inject(UntypedFormBuilder);

    /** Service for managing field properties */
    private fieldPropertyService = inject(FieldPropertyService);

    /** Event emitter for saving field properties */
    @Output() saveField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    /** Event emitter for form validation status */
    @Output() valid: EventEmitter<boolean> = new EventEmitter();

    /** Input data for the form field being edited */
    @Input() formFieldData: DotCMSContentTypeField;

    /** Signal containing the content type information */
    readonly $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    /** Reference to the properties container element */
    @ViewChild('properties') propertiesContainer;

    /** Reactive form group for field properties */
    form: UntypedFormGroup;

    /** Array of field property names to display */
    fieldProperties: string[] = [];

    /** Array of checkbox field names */
    checkboxFields: string[] = ['indexed', 'listed', 'required', 'searchable', 'unique'];

    /** Original form value used for change detection */
    private originalValue: DotCMSContentTypeField;

    /** Subject for managing component destruction and unsubscribing from observables */
    private destroy$: Subject<boolean> = new Subject<boolean>();

    /** Computed signal indicating if the new content editor is enabled */
    $isNewContentEditorEnabled = computed(() => {
        const contentType = this.$contentType();
        return contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
    });

    /**
     * Angular lifecycle hook called when input properties change
     *
     * @param {SimpleChanges} changes - Object containing changed properties
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes.formFieldData?.currentValue && this.formFieldData) {
            this.destroy();

            setTimeout(this.init.bind(this), 0);
        }
    }

    /**
     * Angular lifecycle hook called after component initialization
     */
    ngOnInit(): void {
        // TODO: Migrate to Signal Forms
        this.initFormGroup();
    }

    /**
     * Angular lifecycle hook called before component destruction
     */
    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Emit the form data to be saved
     * Validates the form and marks all fields as touched if invalid
     */
    saveFieldProperties(): void {
        if (this.form.valid) {
            const transformedValue = this.transformFormValue(this.form.value);
            this.saveField.emit(transformedValue);
        } else {
            this.fieldProperties.forEach((property) => this.form.get(property).markAsTouched());
        }

        this.valid.emit(false);
    }

    /**
     * Transform form value before saving
     * Handles special case for custom fields with new render mode variable
     *
     * @param {any} value - The form value to transform
     * @returns {any} The transformed form value
     */
    transformFormValue(value) {
        if (this.formFieldData.clazz === DotCMSClazzes.CUSTOM_FIELD) {
            const existingVariables = this.formFieldData.fieldVariables || [];
            const otherVariables = existingVariables.filter(
                (v) => v.key !== NEW_RENDER_MODE_VARIABLE_KEY
            );
            const existingNewRenderMode = existingVariables.find(
                (v) => v.key === NEW_RENDER_MODE_VARIABLE_KEY
            );
            const newFormValue = {
                ...value,
                fieldVariables: [
                    ...otherVariables,
                    {
                        ...(existingNewRenderMode || {}), // Preserve existing properties (id, etc.)
                        clazz: DotCMSClazzes.FIELD_VARIABLE,
                        key: NEW_RENDER_MODE_VARIABLE_KEY,
                        value:
                            value.newRenderMode || this.fieldPropertyService.$newRenderModeDefault()
                    }
                ]
            };
            return newFormValue;
        }
        return value;
    }

    /**
     * Clean up component state and remove dynamically created property components
     */
    destroy(): void {
        this.fieldProperties = [];

        if (this.propertiesContainer) {
            const propertiesContainer = this.propertiesContainer.nativeElement;
            propertiesContainer.childNodes.forEach((child) => {
                if (child.tagName) {
                    propertiesContainer.removeChild(child);
                }
            });
        }
    }

    /**
     * Initialize the form with field properties
     * Updates form field data, retrieves properties, and initializes form group
     */
    private init(): void {
        this.updateFormFieldData();

        const properties: string[] = this.fieldPropertyService.getProperties(
            this.formFieldData.clazz
        );

        this.initFormGroup(properties);
        this.sortProperties(properties);
    }

    /**
     * Initialize the reactive form group with field properties
     *
     * @param {string[]} [properties] - Optional array of property names to include in the form
     */
    private initFormGroup(properties?: string[]): void {
        const formFields = {};

        if (properties) {
            properties
                .filter((property) => this.fieldPropertyService.existsComponent(property))
                .filter((property) => {
                    if (property === NEW_RENDER_MODE_VARIABLE_KEY) {
                        return this.$isNewContentEditorEnabled();
                    }
                    return true;
                })
                .forEach((property) => {
                    formFields[property] = [
                        {
                            value:
                                this.fieldPropertyService.getValue(this.formFieldData, property) ||
                                this.fieldPropertyService.getDefaultValue(
                                    property,
                                    this.formFieldData.clazz
                                ),
                            disabled: this.formFieldData.id && this.isPropertyDisabled(property)
                        },
                        this.fieldPropertyService.getValidations(property)
                    ];
                });

            formFields['clazz'] = this.formFieldData.clazz;
            formFields['id'] = this.formFieldData.id;
        }

        this.form = this.fb.group(formFields);
        this.setAutoCheckValues();
        this.notifyFormChanges();
    }

    /**
     * Subscribe to form value changes and emit validation status
     * Tracks original value for change detection
     */
    private notifyFormChanges() {
        this.originalValue = this.form.value;
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.valid.emit(this.isFormValueUpdated() && this.form.valid);
        });
    }

    /**
     * Check if the form value has been updated from the original value
     *
     * @returns {boolean} True if the form value differs from the original value
     */
    private isFormValueUpdated(): boolean {
        return !isEqual(this.form.value, this.originalValue);
    }

    /**
     * Check if a property should be disabled in edit mode
     *
     * @param {string} property - The property name to check
     * @returns {boolean} True if the property should be disabled
     */
    private isPropertyDisabled(property: string): boolean {
        return this.fieldPropertyService.isDisabledInEditMode(property);
    }

    /**
     * Sort and filter properties based on component availability and feature flags
     *
     * @param {string[]} properties - Array of property names to sort
     */
    private sortProperties(properties: string[]): void {
        this.fieldProperties = properties
            .filter((property) => this.fieldPropertyService.existsComponent(property))
            .filter((property) => {
                if (property === NEW_RENDER_MODE_VARIABLE_KEY) {
                    return this.$isNewContentEditorEnabled();
                }
                return true;
            })
            .sort(
                (property1, property2) =>
                    this.fieldPropertyService.getOrder(property1) -
                    this.fieldPropertyService.getOrder(property2)
            );
    }

    /**
     * Set up automatic checkbox value handling for searchable, listed, and unique fields
     */
    private setAutoCheckValues(): void {
        [this.form.get('searchable'), this.form.get('listed'), this.form.get('unique')]
            .filter((checkbox) => !!checkbox)
            .forEach((checkbox) => {
                this.handleCheckValues(checkbox);
            });
    }

    /**
     * Handle checkbox value changes and set up value change subscriptions
     *
     * @param {AbstractControl} checkbox - The checkbox form control to handle
     */
    private handleCheckValues(checkbox: AbstractControl): void {
        if (checkbox.value) {
            if (checkbox === this.form.get('unique')) {
                this.handleDisabledRequired(true);
            }

            this.handleDisabledIndexed(true);
        }

        checkbox.valueChanges.subscribe((res) => {
            checkbox === this.form.get('unique')
                ? this.handleUniqueValuesChecked(res)
                : this.setIndexedValueChecked(res);
        });
    }

    /**
     * Set the indexed checkbox value and handle its disabled state
     *
     * @param {boolean} propertyValue - The value to set for the indexed property
     */
    private setIndexedValueChecked(propertyValue: boolean): void {
        if (this.form.get('indexed') && propertyValue) {
            this.form.get('indexed').setValue(propertyValue);
        }

        this.handleDisabledIndexed(propertyValue);
    }

    /**
     * Handle unique checkbox value changes
     * Sets indexed and required values, and manages their disabled states
     *
     * @param {boolean} propertyValue - The value of the unique checkbox
     */
    private handleUniqueValuesChecked(propertyValue: boolean): void {
        this.setIndexedValueChecked(propertyValue);

        if (this.form.get('required') && propertyValue) {
            this.form.get('required').setValue(propertyValue);
        }

        this.handleDisabledRequired(propertyValue);
        this.handleDisabledIndexed(true);
    }

    /**
     * Enable or disable the indexed form control
     *
     * @param {boolean} disable - True to disable, false to enable
     */
    private handleDisabledIndexed(disable: boolean): void {
        if (this.form.get('indexed')) {
            disable ? this.form.get('indexed').disable() : this.form.get('indexed').enable();
        }
    }

    /**
     * Enable or disable the required form control
     *
     * @param {boolean} disable - True to disable, false to enable
     */
    private handleDisabledRequired(disable: boolean): void {
        if (this.form.get('required')) {
            disable ? this.form.get('required').disable() : this.form.get('required').enable();
        }
    }

    /**
     * Update form field data by removing the name property for new fields
     */
    private updateFormFieldData() {
        if (!this.formFieldData.id) {
            delete this.formFieldData['name'];
        }
    }
}
