import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State,
    Watch,
    Host,
    h
} from '@stencil/core';
import {
    DotBinaryFileEvent,
    DotBinaryMessageError,
    DotFieldStatus,
    DotFieldStatusEvent,
    DotFieldValueEvent
} from '../../../models';
import {
    checkProp,
    getClassNames,
    getOriginalStatus,
    getTagError,
    getTagHint,
    isFileAllowed,
    nextTick,
    updateStatus
} from '../../../utils';

import { Components } from '../../../components';
import DotBinaryTextField = Components.DotBinaryTextField;
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';

/**
 * Represent a dotcms binary file control.
 *
 * @export
 * @class DotBinaryFileComponent
 */
@Component({
    tag: 'dot-binary-file',
    styleUrl: 'dot-binary-file.scss'
})
export class DotBinaryFileComponent {
    @Element()
    el: HTMLElement;

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true })
    label = '';

    /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
    @Prop({ reflect: true, mutable: true })
    placeholder = 'Drop or paste a file or url';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** (optional) Determine if it is required */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop()
    requiredMessage = 'This field is required';

    /** (optional) Text that be shown when the Regular Expression condition not met */
    @Prop()
    validationMessage = "The field doesn't comply with the specified format";

    /** (optional) Text that be shown when the URL is not valid */
    @Prop()
    URLValidationMessage = 'The specified URL is not valid';

    /** (optional) Text that be shown when the file size is not valid */
    @Prop()
    fileSizeValidationMessage = 'File size is not valid';

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
    @Prop({ reflect: true, mutable: true })
    accept = '';

    /** (optional) Set the max file size limit  */
    @Prop({ reflect: true, mutable: true })
    maxFileLength = '';

    /** (optional) Text that be shown in the browse file button */
    @Prop({ reflect: true })
    buttonLabel = 'Browse';

    /** (optional) Text that be shown in the browse file button */
    @Prop({ reflect: true, mutable: true })
    errorMessage = '';

    /** (optional) Name of the file uploaded */
    @Prop({ reflect: true, mutable: true })
    previewImageName = '';

    /** (optional) URL of the file uploaded */
    @Prop({ reflect: true, mutable: true })
    previewImageUrl = '';

    @State()
    status: DotFieldStatus;

    @Event()
    dotValueChange: EventEmitter<DotFieldValueEvent>;
    @Event()
    dotStatusChange: EventEmitter<DotFieldStatusEvent>;

    private file: string | File = null;
    private allowedFileTypes = [];
    private errorType: DotBinaryMessageError;
    private binaryTextField: DotBinaryTextField;
    private errorMessageMap = new Map<DotBinaryMessageError, string>();

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        this.file = '';
        this.binaryTextField.value = '';
        this.errorMessage = '';
        this.clearPreviewData();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    /**
     * Clear value of selected file, when the endpoint fails.
     */
    @Method()
    async clearValue(): Promise<void> {
        this.binaryTextField.value = '';
        this.errorType = this.required ? DotBinaryMessageError.REQUIRED : null;
        this.setValue();
        this.clearPreviewData();
    }

    componentWillLoad(): void {
        this.setErrorMessageMap();
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        // this will be null if the component loads with a value
        this.binaryTextField = this.el.querySelector('dot-binary-text-field');
        const attrException = ['dottype'];
        const uploadButtonElement = this.el.querySelector('input[type="file"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(
                Array.from(this.el.attributes),
                attrException
            );
            setDotAttributesToElement(uploadButtonElement, attrs);
        }, 0);
    }

    @Watch('requiredMessage')
    requiredMessageWatch(): void {
        this.errorMessageMap.set(DotBinaryMessageError.REQUIRED, this.requiredMessage);
    }

    @Watch('validationMessage')
    validationMessageWatch(): void {
        this.errorMessageMap.set(DotBinaryMessageError.INVALID, this.validationMessage);
    }

    @Watch('URLValidationMessage')
    URLValidationMessageWatch(): void {
        this.errorMessageMap.set(DotBinaryMessageError.URLINVALID, this.URLValidationMessage);
    }

    @Watch('fileSizeValidationMessage')
    fileSizeValidationMessageWatch(): void {
        this.errorMessageMap.set(DotBinaryMessageError.SIZEINVALID, this.fileSizeValidationMessage);
    }

    @Watch('accept')
    optionsWatch(): void {
        this.accept = checkProp<DotBinaryFileComponent, string>(this, 'accept');

        let arr;
        if (this.accept) {
            arr = this.accept.split(',');

            if (arr.length === 0) {
                arr = [this.accept];
            }
        }

        this.allowedFileTypes = arr ? arr.map((fileType: string) => fileType.trim()) : [];
    }

    @Listen('fileChange')
    fileChangeHandler(event: CustomEvent): void {
        event.stopImmediatePropagation();
        const fileEvent: DotBinaryFileEvent = event.detail;
        this.errorType = fileEvent.errorType;
        this.setValue(fileEvent.file);
        if (this.isBinaryUploadButtonEvent(event.target as Element) && fileEvent.file) {
            this.binaryTextField.value = (fileEvent.file as File).name;
        }
    }

    @Listen('dragover', { passive: false })
    HandleDragover(evt: DragEvent): void {
        evt.preventDefault();
        if (!this.disabled) {
            this.el.classList.add('dot-dragover');
            this.el.classList.remove('dot-dropped');
        }
    }

    @Listen('dragleave', { passive: false })
    HandleDragleave(evt: DragEvent): void {
        evt.preventDefault();
        this.el.classList.remove('dot-dragover');
        this.el.classList.remove('dot-dropped');
    }

    @Listen('drop', { passive: false })
    HandleDrop(evt: DragEvent): void {
        evt.preventDefault();
        this.el.classList.remove('dot-dragover');
        if (!this.disabled && !this.previewImageName) {
            this.el.classList.add('dot-dropped');
            this.errorType = null;
            const droppedFile: File = evt.dataTransfer.files[0];
            this.handleDroppedFile(droppedFile);
        }
    }

    @Listen('delete', { passive: false })
    handleDelete(evt: CustomEvent): void {
        evt.preventDefault();
        this.clearPreviewData();

        /*
            this.binaryTextField could be null if the component loads with a value.
            So we have to wait for `clearPreviewData` happen to bring the <dot-binary-text-field>
            to the DOM so we can get it.
        */
        nextTick(() => {
            if (!this.binaryTextField) {
                this.binaryTextField = this.el.querySelector('dot-binary-text-field');
            }

            this.setValue();
        });
    }

    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);

        return (
            <Host class={{ ...classes }}>
                <dot-label
                    label={this.label}
                    required={this.required}
                    name={this.name}
                    tabindex="0">
                    {this.previewImageName ? (
                        <dot-binary-file-preview
                            onClick={(e: MouseEvent) => {
                                e.preventDefault();
                            }}
                            fileName={this.previewImageName}
                            previewUrl={this.previewImageUrl}
                        />
                    ) : (
                        <div class="dot-binary__container">
                            <dot-binary-text-field
                                placeholder={this.placeholder}
                                required={this.required}
                                disabled={this.disabled}
                                accept={this.allowedFileTypes.join(',')}
                                hint={this.hint}
                                onLostFocus={this.lostFocusEventHandler.bind(this)}
                            />
                            <dot-binary-upload-button
                                name={this.name}
                                accept={this.allowedFileTypes.join(',')}
                                disabled={this.disabled}
                                maxFileLength={this.maxFileLength}
                                required={this.required}
                                buttonLabel={this.buttonLabel}
                            />
                        </div>
                    )}
                </dot-label>
                {getTagHint(this.hint)}
                {getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())}
            </Host>
        );
    }

    private lostFocusEventHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }

    private isBinaryUploadButtonEvent(element: Element): boolean {
        return element.localName === 'dot-binary-upload-button';
    }

    private validateProps(): void {
        this.optionsWatch();
        this.setPlaceHolder();
    }

    private shouldShowErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        return this.errorMessageMap.get(this.errorType);
    }

    private isValid(): boolean {
        return !(this.required && !this.file) && !this.errorType;
    }

    private setErrorMessageMap(): void {
        this.requiredMessageWatch();
        this.validationMessageWatch();
        this.URLValidationMessageWatch();
        this.fileSizeValidationMessageWatch();
    }

    private setValue(data: File | string = null): void {
        try {
            this.file = data;
            this.status = updateStatus(this.status, {
                dotTouched: true,
                dotPristine: false,
                dotValid: this.isValid()
            });
            this.binaryTextField.value = data === null ? '' : this.binaryTextField.value;
        } catch (error) {
            console.warn(error);
        }

        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitStatusChange(): void {
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        this.dotValueChange.emit({
            name: this.name,
            value: this.file
        });
    }

    private handleDroppedFile(file: File): void {
        if (!isFileAllowed(file.name, file.type, this.allowedFileTypes.join(','))) {
            this.errorType = DotBinaryMessageError.INVALID;
            this.setValue();
        } else if (this.maxFileLength ? parseInt(this.maxFileLength, 10) <= file.size : false) {
            this.errorType = DotBinaryMessageError.SIZEINVALID;
            this.setValue();
        } else {
            this.setValue(file);
            this.binaryTextField.value = file.name;
        }
    }

    private setPlaceHolder(): void {
        const DEFAULT_WINDOWS = 'Drop a file or url';
        this.placeholder = this.isWindowsOS() ? DEFAULT_WINDOWS : this.placeholder;
    }

    private isWindowsOS(): boolean {
        return window.navigator.platform.includes('Win');
    }

    private clearPreviewData(): void {
        this.previewImageUrl = '';
        this.previewImageName = '';
    }
}
