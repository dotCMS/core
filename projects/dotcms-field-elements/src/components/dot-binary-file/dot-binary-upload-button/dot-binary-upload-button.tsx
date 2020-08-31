import { Component, Element, Event, EventEmitter, Prop } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotBinaryFileEvent, DotBinaryMessageError } from '../../../models';
import { getId, isFileAllowed } from '../../../utils';

/**
 * Represent a dotcms text field for the binary file element.
 *
 * @export
 * @class DotBinaryFile
 */
@Component({
    tag: 'dot-binary-upload-button',
    styleUrl: 'dot-binary-upload-button.scss'
})
export class DotBinaryUploadButtonComponent {
    @Element() el: HTMLElement;

    /** Name that will be used as ID */
    @Prop({ reflectToAttr: true })
    name = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflectToAttr: true })
    required = false;

    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    @Prop({ reflectToAttr: true })
    accept: string;

    /** (optional) Disables field's interaction */
    @Prop({ reflectToAttr: true })
    disabled = false;

    /** (optional) Text that be shown in the browse file button */
    @Prop({ reflectToAttr: true })
    buttonLabel = '';

    @Event() fileChange: EventEmitter<DotBinaryFileEvent>;

    private fileInput: HTMLInputElement;

    componentDidLoad(): void {
        this.fileInput = this.el.querySelector('dot-label input');
    }

    render() {
        return (
            <Fragment>
                <input
                    accept={this.accept}
                    disabled={this.disabled}
                    id={getId(this.name)}
                    onChange={(event: Event) => this.fileChangeHandler(event)}
                    required={this.required || null}
                    type="file"
                />
                <button type="button" disabled={this.disabled} onClick={() => { this.fileInput.click(); }}>
                    {this.buttonLabel}
                </button>
            </Fragment>
        );
    }

    private fileChangeHandler(event: Event): void {
        const file = this.fileInput.files[0];
        if (isFileAllowed(file.name, this.accept)) {
            this.emitFile(file);
        } else {
            event.preventDefault();
            this.emitFile(null, DotBinaryMessageError.INVALID);
        }
    }

    private emitFile(file: File, errorType?: DotBinaryMessageError): void {
        this.fileChange.emit({
            file: file,
            errorType: errorType
        });
    }
}
