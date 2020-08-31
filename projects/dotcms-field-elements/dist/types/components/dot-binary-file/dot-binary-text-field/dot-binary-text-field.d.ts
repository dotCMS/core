import '../../../stencil.core';
import { EventEmitter } from '../../../stencil.core';
import { DotBinaryFileEvent, DotFieldStatus } from '../../../models';
/**
 * Represent a dotcms text field for the binary file element.
 *
 * @export
 * @class DotBinaryFile
 */
export declare class DotBinaryTextFieldComponent {
    el: HTMLElement;
    /** Value specifies the value of the <input> element */
    value: any;
    /** (optional) Hint text that suggest a clue of the field */
    hint: string;
    /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
    placeholder: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    accept: string;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    status: DotFieldStatus;
    fileChange: EventEmitter<DotBinaryFileEvent>;
    lostFocus: EventEmitter;
    render(): JSX.Element;
    private keyDownHandler;
    private shouldPreventEvent;
    private handleBackspace;
    private pasteHandler;
    private handleFilePaste;
    private handleURLPaste;
    private isPastingFile;
    private isValid;
    private emitFile;
}
