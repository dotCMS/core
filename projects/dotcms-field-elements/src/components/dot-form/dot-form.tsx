import { Component, Element, Event, EventEmitter, Listen, Prop, State } from '@stencil/core';
import { DotCMSContentTypeField } from '../../../../dotcms/src/lib/models';
import { DotFormFields } from './dot-form-fields';

const fieldMap = {
    Text : ((fieldParam: DotCMSContentTypeField) => DotFormFields.Text(fieldParam)),
    Checkbox : ((fieldParam: DotCMSContentTypeField) => DotFormFields.Checkbox(fieldParam)),
    Select : ((fieldParam: DotCMSContentTypeField) => DotFormFields.Select(fieldParam))
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

    @State() _touched = false;
    @State() _pristine = true;
    @State() _valid = true;

    fieldsStatus: {[key: string]: string} = {};

    /**
     * Listen for "valueChanges" and updates the form value with new value.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('valueChange')
    onValueChange(event: CustomEvent): void {
        this.value[event.detail.name] = event.detail.value;
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
        this._pristine = this.getStatusValue('dotPristine');
        this._touched = this.getStatusValue('dotTouched');
        this._valid = this.getStatusValue('dotValid');
    }

    hostData() {
        return {
            class: {
                'dot-valid': this._valid,
                'dot-invalid': !this._valid,
                'dot-pristine': this._pristine,
                'dot-dirty': !this._pristine,
                'dot-touched': this._touched,
                'dot-untouched': !this._touched
            }
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
                <slot />
                {this.fields.map((field: DotCMSContentTypeField) => this.getField(field))}
                <button type='submit' disabled={this._valid ? null : true }>{this.submitLabel}</button>
                <button type='button' onClick={() => this.resetForm()} >{this.resetLabel}</button>
            </form>
        );
    }

    private updateFieldsValues() {
        this.fields = this.fields.map((field: DotCMSContentTypeField) => {
            return typeof this.value[field.variable] !== 'undefined' ? { ...field, defaultValue: this.value[field.variable] } : field;
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
        return this.fieldsToShow.length > 0 && this.fieldsToShow.includes(field.variable);
    }

    private getField(field: DotCMSContentTypeField): any {
        return this.areFieldsToShowDefined(field) || this.fieldsToShow.length === 0 ? this.getFieldTag(field) : '';
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
