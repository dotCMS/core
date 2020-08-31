import '../../../stencil.core';
import { DotCMSContentTypeLayoutColumn } from 'dotcms-models';
export declare class DotFormColumnComponent {
    /** Fields metada to be rendered */
    column: DotCMSContentTypeLayoutColumn;
    /** (optional) List of fields (variableName) separated by comma, to be shown */
    fieldsToShow: string;
    render(): JSX.Element[];
    private getField;
    private getFieldTag;
}
