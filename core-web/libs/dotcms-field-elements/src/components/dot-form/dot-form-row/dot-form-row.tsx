import { Component, Prop, h } from '@stencil/core';

import { DotCMSContentTypeLayoutColumn, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

@Component({
    tag: 'dot-form-row',
    styleUrl: 'dot-form-row.scss'
})
export class DotFormRowComponent {
    /** Fields metada to be rendered */
    @Prop() row: DotCMSContentTypeLayoutRow;

    /** (optional) List of fields (variableName) separated by comma, to be shown */
    @Prop({ reflect: true }) fieldsToShow: string;

    render() {
        // When the user start dragging a form in the edit page the value of layout of the
        // <dot-form> element turns empty and eventually the row prop in this component
        return this.row
            ? this.row.columns.map((fieldColumn: DotCMSContentTypeLayoutColumn) => {
                  return (
                      <dot-form-column column={fieldColumn} fields-to-show={this.fieldsToShow} />
                  );
              })
            : null;
    }
}
