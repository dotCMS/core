import '../../../stencil.core';
import { EventEmitter } from '../../../stencil.core';
import { DotBinaryFileEvent } from '../../../models';
/**
 * Represent a dotcms text field for the binary file element.
 *
 * @export
 * @class DotBinaryFile
 */
export declare class DotBinaryUploadButtonComponent {
    el: HTMLElement;
    /** Name that will be used as ID */
    name: string;
    /** (optional) Determine if it is mandatory */
    required: boolean;
    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    accept: string;
    /** (optional) Disables field's interaction */
    disabled: boolean;
    /** (optional) Text that be shown in the browse file button */
    buttonLabel: string;
    fileChange: EventEmitter<DotBinaryFileEvent>;
    private fileInput;
    componentDidLoad(): void;
    render(): JSX.Element;
    private fileChangeHandler;
    private emitFile;
}
