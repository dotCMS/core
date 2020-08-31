import Fragment from 'stencil-fragment';
import { DotBinaryMessageError } from '../../../models';
import { getErrorClass, getHintId, isFileAllowed, isValidURL } from '../../../utils';
export class DotBinaryTextFieldComponent {
    constructor() {
        this.value = null;
        this.hint = '';
        this.placeholder = '';
        this.required = false;
        this.disabled = false;
    }
    render() {
        return (h(Fragment, null,
            h("input", { type: "text", "aria-describedby": getHintId(this.hint), class: getErrorClass(this.isValid()), disabled: this.disabled, placeholder: this.placeholder, value: this.value, onBlur: () => this.lostFocus.emit(), onKeyDown: (event) => this.keyDownHandler(event), onPaste: (event) => this.pasteHandler(event) })));
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
    static get is() { return "dot-binary-text-field"; }
    static get properties() { return {
        "accept": {
            "type": String,
            "attr": "accept",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        },
        "status": {
            "state": true
        },
        "value": {
            "type": "Any",
            "attr": "value",
            "reflectToAttr": true,
            "mutable": true
        }
    }; }
    static get events() { return [{
            "name": "fileChange",
            "method": "fileChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "lostFocus",
            "method": "lostFocus",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:dot-binary-text-field:**/"; }
}
