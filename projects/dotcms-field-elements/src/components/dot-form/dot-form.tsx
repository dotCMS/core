import { Component, Element, Event, EventEmitter, Listen, Prop } from '@stencil/core';
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
    @Prop() fields: DotCMSContentTypeField[] = [];
    @Prop() fieldsToShow: string[] = [];
    @Prop() resetLabel = 'Reset';
    @Prop() submitLabel = 'Submit';
    @Prop({ mutable: true }) value = {};

    /**
     * Listen for "valueChanges" and updates the form value with new value.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('valueChanges')
    onValueChanges(event: CustomEvent): void {
        this.value[event.detail.name] = event.detail.value;
    }

    /**
     * Listen for "stateChanges" and updates the form status with new value.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    @Listen('stateChanges')
    onStateChanges(event: CustomEvent): void {
        // refresh variables from hostData
    }

    hostData() {
        // TODO: do validation here
        return {
          'class': { 'is-open': this.value },
          'aria-hidden': this.value ? 'false' : 'true'
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
                <button type='submit'>{this.submitLabel}</button>
                <button type='button' onClick={() => this.resetForm()} >{this.resetLabel}</button>
            </form>
        );
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
