import '../../../stencil.core';
import { DotCMSContentTypeLayoutRow } from 'dotcms-models';
export declare class DotFormRowComponent {
    /** Fields metada to be rendered */
    row: DotCMSContentTypeLayoutRow;
    /** (optional) List of fields (variableName) separated by comma, to be shown */
    fieldsToShow: string;
    render(): JSX.Element[];
}
