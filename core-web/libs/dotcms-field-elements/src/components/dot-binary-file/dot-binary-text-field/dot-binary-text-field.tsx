import { Component, Element, Event, EventEmitter, Prop, State, Host, h } from '@stencil/core';

import { DotBinaryFileEvent, DotBinaryMessageError, DotFieldStatus } from '../../../models';
import { getErrorClass, getHintId, isFileAllowed, isValidURL } from '../../../utils';

/**
 * Represent a dotcms text field for the binary file element.
 *
 * @export
 * @class DotBinaryFile
 */
@Component({
    tag: 'dot-binary-text-field',
    styleUrl: 'dot-binary-text-field.scss'
})
export class DotBinaryTextFieldComponent {
    @Element() el: HTMLElement;

    /** Value specifies the value of the <input> element */
    @Prop({ mutable: true, reflect: true })
    value = null;

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
    @Prop({ reflect: true })
    placeholder = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    @Prop({ reflect: true })
    accept: string;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    @State() status: DotFieldStatus;

    @Event() fileChange: EventEmitter<DotBinaryFileEvent>;
    @Event() lostFocus: EventEmitter;

    render() {
        return (
            <Host>
                <input
                    type="text"
                    aria-describedby={getHintId(this.hint)}
                    class={getErrorClass(this.isValid())}
                    disabled={this.disabled}
                    placeholder={this.placeholder}
                    value={this.value}
                    onBlur={() => this.lostFocus.emit()}
                    onKeyDown={(event: KeyboardEvent) => this.keyDownHandler(event)}
                    onPaste={(event: ClipboardEvent) => this.pasteHandler(event)}
                />
            </Host>
        );
    }

    private keyDownHandler(evt: KeyboardEvent): void {
        if (evt.key === 'Backspace') {
            this.handleBackspace();
        } else if (this.shouldPreventEvent(evt)) {
            evt.preventDefault();
        }
    }

    private shouldPreventEvent(evt: KeyboardEvent): boolean {
        return !(evt.ctrlKey || evt.metaKey);
    }

    private handleBackspace(): void {
        this.value = '';
        this.emitFile(null, this.required ? DotBinaryMessageError.REQUIRED : null);
    }

    // only supported in macOS.
    private pasteHandler(event: ClipboardEvent): void {
        event.preventDefault();
        this.value = '';
        const clipboardData: DataTransfer = event.clipboardData;
        if (clipboardData.items.length) {
            if (this.isPastingFile(clipboardData)) {
                this.handleFilePaste(clipboardData.items);
            } else {
                const clipBoardFileName = clipboardData.items[0];
                this.handleURLPaste(clipBoardFileName);
            }
        }
    }

    private handleFilePaste(items: DataTransferItemList) {
        const clipBoardFileName = items[0];
        const clipBoardFile = items[1].getAsFile();
        clipBoardFileName.getAsString((fileName: string) => {
            if (isFileAllowed(fileName, this.accept)) {
                this.value = fileName;
                this.emitFile(clipBoardFile);
            } else {
                this.emitFile(null, DotBinaryMessageError.INVALID);
            }
        });
    }

    private handleURLPaste(clipBoardFileName: DataTransferItem) {
        clipBoardFileName.getAsString((fileURL: string) => {
            if (isValidURL(fileURL)) {
                this.value = fileURL;
                this.emitFile(fileURL);
            } else {
                this.emitFile(null, DotBinaryMessageError.URLINVALID);
            }
        });
    }

    private isPastingFile(data: DataTransfer): boolean {
        return !!data.files.length;
    }

    private isValid(): boolean {
        return !(this.required && !!this.value);
    }

    private emitFile(file: File | string, errorType?: DotBinaryMessageError): void {
        this.fileChange.emit({
            file: file,
            errorType: errorType
        });
    }
}
