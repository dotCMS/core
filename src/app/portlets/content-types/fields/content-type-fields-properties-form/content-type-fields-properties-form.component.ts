import {
  Component,
  Output,
  EventEmitter,
  Input,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ComponentFactoryResolver,
  ComponentRef,
  OnChanges,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import { FormGroup, FormControl, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { MessageService } from '../../../../api/services/messages-service';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Field } from '../shared';
import { FieldPropertyService } from '../service/';


@Component({
    selector: 'content-type-fields-properties-form',
    styleUrls: ['./content-type-fields-properties-form.component.scss'],
    templateUrl: './content-type-fields-properties-form.component.html',
    encapsulation: ViewEncapsulation.None
})

export class ContentTypeFieldsPropertiesFormComponent extends BaseComponent implements OnChanges, OnInit {
    @Output() saveField: EventEmitter<any> = new EventEmitter();
    @Input() formFieldData: Field;

    @ViewChild('properties') propertiesContainer;

    form: FormGroup;
    fieldProperties: string[] = [];
    checkboxFields: string[] = ['indexed', 'listed', 'required', 'searchable', 'unique'];

    constructor(private fb: FormBuilder, private componentFactoryResolver: ComponentFactoryResolver,
        public messageService: MessageService, private fieldPropertyService: FieldPropertyService) {

        super(
            [
                'contenttypes.field.properties.name.label',
                'contenttypes.field.properties.category.label',
                'contenttypes.field.properties.required.label',
                'contenttypes.field.properties.user_searchable.label',
                'contenttypes.field.properties.system_indexed.label',
                'contenttypes.field.properties.listed.label',
                'contenttypes.field.properties.unique.label',
                'contenttypes.field.properties.default_value.label',
                'contenttypes.field.properties.hint.label',
                'contenttypes.field.properties.validation_regex.label',
                'contenttypes.field.properties.data_type.label',
                'contenttypes.field.properties.value.label',
                'contenttypes.field.properties.data_type.values.binary',
                'contenttypes.field.properties.data_type.values.text',
                'contenttypes.field.properties.data_type.values.boolean',
                'contenttypes.field.properties.data_type.values.date',
                'contenttypes.field.properties.data_type.values.decimal',
                'contenttypes.field.properties.data_type.values.number',
                'contenttypes.field.properties.data_type.values.large_text',
                'contenttypes.field.properties.data_type.values.system',
                'contenttypes.field.properties.category.error.required',
                'contenttypes.field.properties.default_value..error.format',
                'contenttypes.field.properties.name.error.required',
                'contenttypes.field.properties.validation_regex.values.select',
                'contenttypes.field.properties.validation_regex.values.no_html',
                'contenttypes.field.properties.validation_regex.values.us_phone',
                'contenttypes.field.properties.validation_regex.values.us_zip_code',
                'contenttypes.field.properties.validation_regex.values.letters_only',
                'contenttypes.field.properties.validation_regex.values.numbers_only',
                'contenttypes.field.properties.validation_regex.values.email',
                'contenttypes.field.properties.validation_regex.values.alphanumeric',
                'contenttypes.field.properties.validation_regex.values.url_pattern',
            ],
            messageService
        );
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.formFieldData.currentValue && this.formFieldData) {

            if (!this.formFieldData.id) {
                delete this.formFieldData['name'];
            }

            const properties: string[] = this.fieldPropertyService.getProperties(this.formFieldData.clazz);
            this.initFormGroup(properties);
            this.sortProperties(properties);
        }
    }

    ngOnInit(): void {
        this.initFormGroup();
    }

    /**
     * Emit the form data to be saved
     *
     * @memberof ContentTypeFieldsPropertiesFormComponent
     */
    saveFieldProperties(): void {
        if (this.form.valid) {
             this.saveField.emit(this.form.value);
        } else {
            this.fieldProperties.forEach(property => this.form.get(property).markAsTouched());
        }
    }

    public destroy(): void {
        this.fieldProperties = [];
        const propertiesContainer = this.propertiesContainer.nativeElement;
        propertiesContainer.childNodes.forEach(child => {
            if (child.tagName) {
                propertiesContainer.removeChild(child);
            }
        });
    }

    private initFormGroup(properties?: string[]): void {
        const formFields = {};

        if (properties) {
            properties.filter(property => this.fieldPropertyService.existsComponent(property))
                .forEach(property => {
                    formFields[property] = [{
                        value: this.formFieldData[property] ||
                                this.fieldPropertyService.getDefaultValue(property, this.formFieldData.clazz),
                        disabled: this.formFieldData.id && this.fieldPropertyService.isDisabledInEditMode(property)
                    }, this.fieldPropertyService.getValidations(property)];
                });

            formFields['clazz'] = this.formFieldData.clazz;
        }
        this.form = this.fb.group(formFields);

        this.setAutoCheckValues();
    }

    private sortProperties(properties: string[]): void {
        this.fieldProperties = properties.filter(property => this.fieldPropertyService.existsComponent(property))
        .sort((property1, proeprty2) =>
            this.fieldPropertyService.getOrder(property1) - this.fieldPropertyService.getOrder(proeprty2));

    }

    private setAutoCheckValues(): void {
        if (this.form.get('searchable')) {
            this.handleCheckValues(this.form.get('searchable'));
        }
        if (this.form.get('listed')) {
            this.handleCheckValues(this.form.get('listed'));
        }
        if (this.form.get('unique')) {
            this.handleCheckValues(this.form.get('unique'));
        }
    }

    private handleCheckValues(checkbox: AbstractControl): void {
        if (checkbox.value) {
            if (checkbox === this.form.get('unique')) {
                this.handleDisabledRequired(true);
            }
            this.handleDisabledIndexed(true);
        }

        checkbox.valueChanges.subscribe(res => {
            checkbox === this.form.get('unique') ? this.handleUniqueValuesChecked(res) : this.setIndexedValueChecked(res);
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
    }

    private handleDisabledIndexed(disable: boolean): void {
        disable ? this.form.get('indexed').disable() : this.form.get('indexed').enable();
    }

    private handleDisabledRequired(disable: boolean): void {
        disable ? this.form.get('required').disable() : this.form.get('required').enable();
    }
}
