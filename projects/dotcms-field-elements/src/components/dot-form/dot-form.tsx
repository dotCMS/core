import { Component, Element, Event, EventEmitter, Listen, Prop, State } from '@stencil/core';
import { DotCMSContentTypeField, DotFieldStatus, DotCMSKeyValueField } from '../../models';
import { DotFormFields } from './dot-form-fields';
import { getClassNames, getOriginalStatus, getStringFromDotKeyArray, updateStatus } from '../../utils';

const fieldParamsConversionToBE = {
    'Key-Value': (values: string): { [key: string]: string } => {
        const returnValue = {};
        values.split(',').forEach((item: string) => {
            const [key, value] = item.split('|');
            returnValue[key] = value;
        });
        return returnValue;
    }
};

const fieldParamsConversionFromBE = {
    'Key-Value': (field: DotCMSKeyValueField) => {
        if (field.defaultValue && typeof field.defaultValue !== 'string') {
            const valuesArray = Object.keys(field.defaultValue).map((key: string) => {
                return {key: key, value: field.defaultValue[key]};
            });
            field.defaultValue = getStringFromDotKeyArray(valuesArray);
        }
        return DotFormFields['Key-Value'](field);
    }
};

const fieldMap = {
    Text: DotFormFields.Text,
    Textarea: DotFormFields.Textarea,
    Checkbox: DotFormFields.Checkbox,
    'Key-Value': fieldParamsConversionFromBE['Key-Value'],
    'Multi-Select': DotFormFields['Multi-Select'],
    Select: DotFormFields.Select,
    Radio: DotFormFields.Radio,
    Date: DotFormFields.Date,
    Time: DotFormFields.Time,
    'Date-Range': DotFormFields['Date-Range']
};

@Component({
    tag: 'dot-form'
})
export class DotFormComponent {
    @Element() el: HTMLElement;

    @Event() onSubmit: EventEmitter;

    @Prop({ mutable: true }) fields: DotCMSContentTypeField[] = [];
    @Prop() fieldsToShow: string[] = [];
    @Prop() resetLabel = 'Reset';
    @Prop() submitLabel = 'Submit';
    @Prop({ mutable: true }) value = {};

    @State() status: DotFieldStatus = getOriginalStatus();

    fieldsStatus: { [key: string]: string } = {};

    /**
     * Listen for "valueChanges" and updates the form value with new value.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('valueChange')
    onValueChange(event: CustomEvent): void {
        const { name, value, fieldType } = event.detail;
        this.value[name] = fieldType ? fieldParamsConversionToBE[fieldType](value) : value;
        this.updateFieldsValues();
    }

    /**
     * Listen for "statusChange" and updates the form status with new value.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('statusChange')
    onStatusChange(event: CustomEvent): void {
        this.fieldsStatus[event.detail.name] = event.detail.status;
        this.status = updateStatus(this.status, {
            dotTouched: this.getStatusValue('dotTouched'),
            dotPristine: this.getStatusValue('dotPristine'),
            dotValid: this.getStatusValue('dotValid')
        });
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.status.dotValid)
        };
    }

    componentWillLoad() {
        this.fields.forEach((field: DotCMSContentTypeField) => {
            if (this.getFieldTag(field)) {
                this.value[field.variable] = field.defaultValue || '';
            }
        });
    }

    render() {
        return (
            <form onSubmit={(evt: Event) => this.handleSubmit(evt)}>
                {this.fields.map((field: DotCMSContentTypeField) => this.getField(field))}
                <button type="submit" disabled={!this.status.dotValid || null}>
                    {this.submitLabel}
                </button>
                <button type="button" onClick={() => this.resetForm()}>
                    {this.resetLabel}
                </button>
            </form>
        );
    }

    private updateFieldsValues() {
        this.fields = this.fields.map((field: DotCMSContentTypeField) => {
            return typeof this.value[field.variable] !== 'undefined'
                ? { ...field, defaultValue: this.value[field.variable] }
                : field;
        });
    }

    private getStatusValue(statusName: string): boolean {
        let value;
        const fields = Object.keys(this.fieldsStatus);
        for (const field of fields) {
            if (!this.fieldsStatus[field][statusName]) {
                value = this.fieldsStatus[field][statusName];
                break;
            }
            value = this.fieldsStatus[field][statusName];
        }
        return value;
    }

    private handleSubmit(evt: Event): void {
        evt.preventDefault();
        this.onSubmit.emit({
            ...this.value
        });
    }

    private getFieldTag(field: DotCMSContentTypeField): any {
        return fieldMap[field.fieldType] ? fieldMap[field.fieldType](field) : '';
    }

    private areFieldsToShowDefined(field: DotCMSContentTypeField): boolean {
        return this.fieldsToShow.length === 0 || this.fieldsToShow.includes(field.variable);
    }

    private getField(field: DotCMSContentTypeField): any {
        return this.areFieldsToShowDefined(field) ? this.getFieldTag(field) : '';
    }

    private resetForm(): void {
        const elements = Array.from(this.el.querySelectorAll('form > *:not(button)'));
        elements.forEach((element: any) => {
            try {
                element.reset();
            } catch (error) {
                console.error('Error:', error);
            }
        });
    }
}
