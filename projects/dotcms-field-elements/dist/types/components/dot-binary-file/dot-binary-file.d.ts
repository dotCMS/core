import '../../stencil.core';
import { EventEmitter } from '../../stencil.core';
import { DotFieldStatus, DotFieldStatusEvent, DotFieldValueEvent } from '../../models';
/**
 * Represent a dotcms binary file control.
 *
 * @export
 * @class DotBinaryFile
 */
export declare class DotBinaryFileComponent {
    el: HTMLElement;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Text to be rendered next to input field */
    label: string;
    /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
    placeholder: string;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Text that be shown when required is set and condition not met */
    requiredMessage: string;
    /** (optional) Text that be shown when the Regular Expression condition not met */
    validationMessage: string;
    /** (optional) Text that be shown when the URL is not valid */
    URLValidationMessage: string;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    accept: string;
    /** (optional) Set the max file size limit  */
    maxFileLength: string;
    /** (optional) Text that be shown in the browse file button */
    buttonLabel: string;
    /** (optional) Text that be shown in the browse file button */
    errorMessage: string;
    /** (optional) Name of the file uploaded */
    previewImageName: string;
    /** (optional) URL of the file uploaded */
    previewImageUrl: string;
    status: DotFieldStatus;
    valueChange: EventEmitter<DotFieldValueEvent>;
    statusChange: EventEmitter<DotFieldStatusEvent>;
    private file;
    private allowedFileTypes;
    private errorType;
    private binaryTextField;
    private errorMessageMap;
    /**
     * Reset properties of the field, clear value and emit events.
     */
    reset(): void;
    /**
     * Clear value of selected file, when the endpoint fails.
     */
    clearValue(): void;
    componentWillLoad(): void;
    componentDidLoad(): void;
    requiredMessageWatch(): void;
    validationMessageWatch(): void;
    URLValidationMessageWatch(): void;
    optionsWatch(): void;
    fileChangeHandler(event: CustomEvent): void;
    HandleDragover(evt: DragEvent): void;
    HandleDragleave(evt: DragEvent): void;
    HandleDrop(evt: DragEvent): void;
    handleDelete(evt: CustomEvent): void;
    hostData(): {
        class: import("../../models").DotFieldStatusClasses;
    };
    render(): JSX.Element;
    private lostFocusEventHandler;
    private isBinaryUploadButtonEvent;
    private validateProps;
    private shouldShowErrorMessage;
    private getErrorMessage;
    private isValid;
    private setErrorMessageMap;
    private setValue;
    private emitStatusChange;
    private emitValueChange;
    private handleDroppedFile;
    private setPlaceHolder;
    private isWindowsOS;
    private clearPreviewData;
}
