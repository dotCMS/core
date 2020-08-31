import '../../stencil.core';
import { DotFieldStatus } from '../../models';
import { DotCMSContentTypeLayoutRow } from 'dotcms-models';
export declare class DotFormComponent {
    el: HTMLElement;
    /** (optional) List of fields (variableName) separated by comma, to be shown */
    fieldsToShow: string;
    /** (optional) Text to be rendered on Reset button */
    resetLabel: string;
    /** (optional) Text to be rendered on Submit button */
    submitLabel: string;
    /** Layout metada to be rendered */
    layout: DotCMSContentTypeLayoutRow[];
    /** Content type variable name */
    variable: string;
    status: DotFieldStatus;
    errorMessage: string;
    uploadFileInProgress: boolean;
    private fieldsStatus;
    private value;
    /**
     * Update the form value when valueChange in any of the child fields.
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    onValueChange(event: CustomEvent): void;
    /**
     * Update the form status when statusChange in any of the child fields
     *
     * @param CustomEvent event
     * @memberof DotFormComponent
     */
    onStatusChange({ detail }: CustomEvent): void;
    layoutWatch(): void;
    fieldsToShowWatch(): void;
    hostData(): {
        class: import("../../models").DotFieldStatusClasses;
    };
    componentWillLoad(): void;
    render(): JSX.Element;
    private getStatusValueByName;
    private getTouched;
    private handleSubmit;
    private runSuccessCallback;
    private getSuccessCallback;
    private resetForm;
    private getUpdateValue;
    private getMaxSize;
    private uploadFile;
}
