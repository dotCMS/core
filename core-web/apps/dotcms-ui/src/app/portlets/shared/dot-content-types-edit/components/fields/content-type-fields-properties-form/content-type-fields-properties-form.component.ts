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
    inject
} from '@angular/core';
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';

import { takeUntil } from 'rxjs/operators';

import { DotCMSClazzes, DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { isEqual } from '@dotcms/utils';

import { FieldPropertyService } from '../service';

@Component({
    selector: 'dot-content-type-fields-properties-form',
    styleUrls: ['./content-type-fields-properties-form.component.scss'],
    templateUrl: './content-type-fields-properties-form.component.html',
    standalone: false
})
export class ContentTypeFieldsPropertiesFormComponent implements OnChanges, OnInit, OnDestroy {
    private fb = inject(UntypedFormBuilder);
    private fieldPropertyService = inject(FieldPropertyService);

    @Output() saveField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    @Output() valid: EventEmitter<boolean> = new EventEmitter();

    @Input() formFieldData: DotCMSContentTypeField;

    @Input() contentType: DotCMSContentType;

    @ViewChild('properties') propertiesContainer;

    form: UntypedFormGroup;
    fieldProperties: string[] = [];
    checkboxFields: string[] = ['indexed', 'listed', 'required', 'searchable', 'unique'];

    private originalValue: DotCMSContentTypeField;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.formFieldData?.currentValue && this.formFieldData) {
            this.destroy();

            setTimeout(this.init.bind(this), 0);
        }
    }

    ngOnInit(): void {
        this.initFormGroup();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Emit the form data to be saved
     *
     * @memberof ContentTypeFieldsPropertiesFormComponent
     */
    saveFieldProperties(): void {
        if (this.form.valid) {
            this.saveField.emit(this.transformFormValue(this.form.value));
        } else {
            this.fieldProperties.forEach((property) => this.form.get(property).markAsTouched());
        }

        this.valid.next(false);
    }

    transformFormValue(value: any): any {
        console.log('value', value);
        console.log('this.formFieldData.clazz', this.formFieldData.clazz);
        if (this.formFieldData.clazz === DotCMSClazzes.CUSTOM_FIELD) {
            console.log('value.newRenderMode', value.newRenderMode);
            return {
                ...value,
                fieldVariables: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        key: 'newRenderMode',
                        value: value.newRenderMode === 'true'
                    }
                ]
            };
        }
        return value;
    }

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

    private init(): void {
        this.updateFormFieldData();

        const properties: string[] = this.fieldPropertyService.getProperties(
            this.formFieldData.clazz
        );

        this.initFormGroup(properties);
        this.sortProperties(properties);
    }

    private initFormGroup(properties?: string[]): void {
        const formFields = {};

        if (properties) {
            properties
                .filter((property) => this.fieldPropertyService.existsComponent(property))
                .forEach((property) => {
                    formFields[property] = [
                        {
                            value:
                                this.formFieldData[property] ||
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

    private notifyFormChanges() {
        this.originalValue = this.form.value;
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.valid.next(this.isFormValueUpdated() && this.form.valid);
        });
    }

    private isFormValueUpdated(): boolean {
        return !isEqual(this.form.value, this.originalValue);
    }

    private isPropertyDisabled(property: string): boolean {
        return this.fieldPropertyService.isDisabledInEditMode(property);
    }

    private sortProperties(properties: string[]): void {
        this.fieldProperties = properties
            .filter((property) => this.fieldPropertyService.existsComponent(property))
            .sort(
                (property1, proeprty2) =>
                    this.fieldPropertyService.getOrder(property1) -
                    this.fieldPropertyService.getOrder(proeprty2)
            );
    }

    private setAutoCheckValues(): void {
        [this.form.get('searchable'), this.form.get('listed'), this.form.get('unique')]
            .filter((checkbox) => !!checkbox)
            .forEach((checkbox) => {
                this.handleCheckValues(checkbox);
            });
    }

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

    private setIndexedValueChecked(propertyValue: boolean): void {
        if (this.form.get('indexed') && propertyValue) {
            this.form.get('indexed').setValue(propertyValue);
        }

        this.handleDisabledIndexed(propertyValue);
    }

    private handleUniqueValuesChecked(propertyValue: boolean): void {
        this.setIndexedValueChecked(propertyValue);

        if (this.form.get('required') && propertyValue) {
            this.form.get('required').setValue(propertyValue);
        }

        this.handleDisabledRequired(propertyValue);
        this.handleDisabledIndexed(true);
    }

    private handleDisabledIndexed(disable: boolean): void {
        if (this.form.get('indexed')) {
            disable ? this.form.get('indexed').disable() : this.form.get('indexed').enable();
        }
    }

    private handleDisabledRequired(disable: boolean): void {
        if (this.form.get('required')) {
            disable ? this.form.get('required').disable() : this.form.get('required').enable();
        }
    }

    private updateFormFieldData() {
        if (!this.formFieldData.id) {
            delete this.formFieldData['name'];
        }
    }
}
