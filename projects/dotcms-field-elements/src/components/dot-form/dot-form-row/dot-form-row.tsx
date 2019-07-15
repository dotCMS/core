import { Component, Prop } from '@stencil/core';
import { DotCMSContentTypeLayoutColumn, DotCMSContentTypeLayoutRow } from 'dotcms-models';

@Component({
    tag: 'dot-form-row',
    styleUrl: 'dot-form-row.scss'
})
export class DotFormRowComponent {
    /** Fields metada to be rendered */
    @Prop() row: DotCMSContentTypeLayoutRow;

    /** (optional) List of fields (variableName) separated by comma, to be shown */
    @Prop({ reflectToAttr: true }) fieldsToShow: string;

    render() {
        return this.row.columns.map((fieldColumn: DotCMSContentTypeLayoutColumn) => {
            return <dot-form-column column={fieldColumn} fields-to-show={this.fieldsToShow} />;
        });
    }
}
