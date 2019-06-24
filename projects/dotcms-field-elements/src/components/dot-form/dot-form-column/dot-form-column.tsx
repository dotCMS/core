import { Component, Prop } from '@stencil/core';
import { DotCMSContentTypeColumn, DotCMSContentTypeField } from '../models';
import { fieldMap, shouldShowField } from '../utils';

@Component({
    tag: 'dot-form-column',
    styleUrl: 'dot-form-column.scss'
})
export class DotFormColumnComponent {
    /** Fields metada to be rendered */
    @Prop() column: DotCMSContentTypeColumn;

    /** (optional) List of fields (variableName) separated by comma, to be shown */
    @Prop({ reflectToAttr: true }) fieldsToShow: string;

    render() {
        return this.column.fields.map((field: DotCMSContentTypeField) => this.getField(field));
    }

    private getField(field: DotCMSContentTypeField): JSX.Element {
        return shouldShowField(field, this.fieldsToShow) ? this.getFieldTag(field) : '';
    }

    private getFieldTag(field: DotCMSContentTypeField): JSX.Element {
        return fieldMap[field.fieldType] ? fieldMap[field.fieldType](field) : '';
    }
}
