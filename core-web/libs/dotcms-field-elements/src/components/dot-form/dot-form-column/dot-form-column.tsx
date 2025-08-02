import { Component, Prop } from '@stencil/core';
import { fieldMap, shouldShowField } from '../utils';
import { DotCMSContentTypeLayoutColumn, DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    tag: 'dot-form-column',
    styleUrl: 'dot-form-column.scss'
})
export class DotFormColumnComponent {
    /** Fields metada to be rendered */
    @Prop() column: DotCMSContentTypeLayoutColumn;

    /** (optional) List of fields (variableName) separated by comma, to be shown */
    @Prop({ reflect: true }) fieldsToShow: string;

    render() {
        // When the user start dragging a form in the edit page the value of layout of the
        // <dot-form> element turns empty and eventually the column prop in this component
        return this.column
            ? this.column.fields.map((field: DotCMSContentTypeField) => this.getField(field))
            : null;
    }

    private getField(field: DotCMSContentTypeField) {
        return shouldShowField(field, this.fieldsToShow) ? this.getFieldTag(field) : null;
    }

    private getFieldTag(field: DotCMSContentTypeField) {
        return fieldMap[field.fieldType] ? fieldMap[field.fieldType](field) : '';
    }
}
