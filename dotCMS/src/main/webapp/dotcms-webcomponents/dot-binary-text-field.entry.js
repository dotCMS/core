import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { d as getHintId, e as getErrorClass, l as isFileAllowed, n as isValidURL } from './index-d52678cd.js';
import { D as DotBinaryMessageError } from './index-2f031ed5.js';

const DotBinaryTextFieldComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value specifies the value of the <input> element */
        this.value = null;
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
        this.placeholder = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Disables field's interaction */
        this.disabled = false;
        this.fileChange = createEvent(this, "fileChange", 7);
        this.lostFocus = createEvent(this, "lostFocus", 7);
    }
    render() {
        return (h(Host, null, h("input", { type: "text", "aria-describedby": getHintId(this.hint), class: getErrorClass(this.isValid()), disabled: this.disabled, placeholder: this.placeholder, value: this.value, onBlur: () => this.lostFocus.emit(), onKeyDown: (event) => this.keyDownHandler(event), onPaste: (event) => this.pasteHandler(event) })));
    }
    keyDownHandler(evt) {
        if (evt.key === 'Backspace') {
            this.handleBackspace();
        }
        else if (this.shouldPreventEvent(evt)) {
            evt.preventDefault();
        }
    }
    shouldPreventEvent(evt) {
        return !(evt.ctrlKey || evt.metaKey);
    }
    handleBackspace() {
        this.value = '';
        this.emitFile(null, this.required ? DotBinaryMessageError.REQUIRED : null);
    }
    // only supported in macOS.
    pasteHandler(event) {
        event.preventDefault();
        this.value = '';
        const clipboardData = event.clipboardData;
        if (clipboardData.items.length) {
            if (this.isPastingFile(clipboardData)) {
                this.handleFilePaste(clipboardData.items);
            }
            else {
                const clipBoardFileName = clipboardData.items[0];
                this.handleURLPaste(clipBoardFileName);
            }
        }
    }
    handleFilePaste(items) {
        const clipBoardFileName = items[0];
        const clipBoardFile = items[1].getAsFile();
        clipBoardFileName.getAsString((fileName) => {
            if (isFileAllowed(fileName, this.accept)) {
                this.value = fileName;
                this.emitFile(clipBoardFile);
            }
            else {
                this.emitFile(null, DotBinaryMessageError.INVALID);
            }
        });
    }
    handleURLPaste(clipBoardFileName) {
        clipBoardFileName.getAsString((fileURL) => {
            if (isValidURL(fileURL)) {
                this.value = fileURL;
                this.emitFile(fileURL);
            }
            else {
                this.emitFile(null, DotBinaryMessageError.URLINVALID);
            }
        });
    }
    isPastingFile(data) {
        return !!data.files.length;
    }
    isValid() {
        return !(this.required && !!this.value);
    }
    emitFile(file, errorType) {
        this.fileChange.emit({
            file: file,
            errorType: errorType
        });
    }
    get el() { return getElement(this); }
    static get style() { return ""; }
};

export { DotBinaryTextFieldComponent as dot_binary_text_field };
