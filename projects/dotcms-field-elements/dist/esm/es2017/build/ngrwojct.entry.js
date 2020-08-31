import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, b as checkProp, c as getClassNames, d as getTagHint, e as getTagError, f as updateStatus, g as isFileAllowed, h as getHintId, i as getErrorClass, j as isValidURL, k as getId } from './chunk-62cd3eff.js';
import { a as getDotAttributesFromElement, b as setDotAttributesToElement } from './chunk-4205a04e.js';

var DotBinaryMessageError;
(function (DotBinaryMessageError) {
    DotBinaryMessageError[DotBinaryMessageError["REQUIRED"] = 0] = "REQUIRED";
    DotBinaryMessageError[DotBinaryMessageError["INVALID"] = 1] = "INVALID";
    DotBinaryMessageError[DotBinaryMessageError["URLINVALID"] = 2] = "URLINVALID";
})(DotBinaryMessageError || (DotBinaryMessageError = {}));

class DotBinaryFileComponent {
    constructor() {
        this.name = '';
        this.label = '';
        this.placeholder = 'Drop or paste a file or url';
        this.hint = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.validationMessage = "The field doesn't comply with the specified format";
        this.URLValidationMessage = 'The specified URL is not valid';
        this.disabled = false;
        this.accept = '';
        this.maxFileLength = '';
        this.buttonLabel = 'Browse';
        this.errorMessage = '';
        this.previewImageName = '';
        this.previewImageUrl = '';
        this.file = null;
        this.allowedFileTypes = [];
        this.errorMessageMap = new Map();
    }
    reset() {
        this.file = '';
        this.binaryTextField.value = '';
        this.errorMessage = '';
        this.clearPreviewData();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }
    clearValue() {
        this.binaryTextField.value = '';
        this.errorType = this.required ? DotBinaryMessageError.REQUIRED : null;
        this.setValue('');
        this.clearPreviewData();
    }
    componentWillLoad() {
        this.setErrorMessageMap();
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        this.binaryTextField = this.el.querySelector('dot-binary-text-field');
        const attrException = ['dottype'];
        const uploadButtonElement = this.el.querySelector('input[type="file"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), attrException);
            setDotAttributesToElement(uploadButtonElement, attrs);
        }, 0);
    }
    requiredMessageWatch() {
        this.errorMessageMap.set(DotBinaryMessageError.REQUIRED, this.requiredMessage);
    }
    validationMessageWatch() {
        this.errorMessageMap.set(DotBinaryMessageError.INVALID, this.validationMessage);
    }
    URLValidationMessageWatch() {
        this.errorMessageMap.set(DotBinaryMessageError.URLINVALID, this.URLValidationMessage);
    }
    optionsWatch() {
        this.accept = checkProp(this, 'accept');
        this.allowedFileTypes = !!this.accept ? this.accept.split(',') : [];
        this.allowedFileTypes = this.allowedFileTypes.map((fileType) => fileType.trim());
    }
    fileChangeHandler(event) {
        event.stopImmediatePropagation();
        const fileEvent = event.detail;
        this.errorType = fileEvent.errorType;
        this.setValue(fileEvent.file);
        if (this.isBinaryUploadButtonEvent(event.target) && fileEvent.file) {
            this.binaryTextField.value = fileEvent.file.name;
        }
    }
    HandleDragover(evt) {
        evt.preventDefault();
        if (!this.disabled) {
            this.el.classList.add('dot-dragover');
            this.el.classList.remove('dot-dropped');
        }
    }
    HandleDragleave(evt) {
        evt.preventDefault();
        this.el.classList.remove('dot-dragover');
        this.el.classList.remove('dot-dropped');
    }
    HandleDrop(evt) {
        evt.preventDefault();
        this.el.classList.remove('dot-dragover');
        if (!this.disabled && !this.previewImageName) {
            this.el.classList.add('dot-dropped');
            this.errorType = null;
            const droppedFile = evt.dataTransfer.files[0];
            this.handleDroppedFile(droppedFile);
        }
    }
    handleDelete(evt) {
        evt.preventDefault();
        this.setValue('');
        this.clearPreviewData();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name, tabindex: "0" }, this.previewImageName ? (h("dot-binary-file-preview", { onClick: (e) => {
                    e.preventDefault();
                }, fileName: this.previewImageName, previewUrl: this.previewImageUrl })) : (h("div", { class: "dot-binary__container" },
                h("dot-binary-text-field", { placeholder: this.placeholder, required: this.required, disabled: this.disabled, accept: this.allowedFileTypes.join(','), hint: this.hint, onLostFocus: this.lostFocusEventHandler.bind(this) }),
                h("dot-binary-upload-button", { name: this.name, accept: this.allowedFileTypes.join(','), disabled: this.disabled, required: this.required, buttonLabel: this.buttonLabel })))),
            getTagHint(this.hint),
            getTagError(this.shouldShowErrorMessage(), this.getErrorMessage()),
            h("dot-error-message", null, this.errorMessage)));
    }
    lostFocusEventHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    isBinaryUploadButtonEvent(element) {
        return element.localName === 'dot-binary-upload-button';
    }
    validateProps() {
        this.optionsWatch();
        this.setPlaceHolder();
    }
    shouldShowErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.errorMessageMap.get(this.errorType);
    }
    isValid() {
        return !(this.required && !this.file);
    }
    setErrorMessageMap() {
        this.requiredMessageWatch();
        this.validationMessageWatch();
        this.URLValidationMessageWatch();
    }
    setValue(data) {
        this.file = data;
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.binaryTextField.value = data === null ? '' : this.binaryTextField.value;
        this.emitValueChange();
        this.emitStatusChange();
    }
    emitStatusChange() {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.valueChange.emit({
            name: this.name,
            value: this.file
        });
    }
    handleDroppedFile(file) {
        if (isFileAllowed(file.name, this.allowedFileTypes.join(','))) {
            this.setValue(file);
            this.binaryTextField.value = file.name;
        }
        else {
            this.errorType = DotBinaryMessageError.INVALID;
            this.setValue(null);
        }
    }
    setPlaceHolder() {
        const DEFAULT_WINDOWS = 'Drop a file or url';
        this.placeholder = this.isWindowsOS() ? DEFAULT_WINDOWS : this.placeholder;
    }
    isWindowsOS() {
        return window.navigator.platform.includes('Win');
    }
    clearPreviewData() {
        this.previewImageUrl = '';
        this.previewImageName = '';
    }
    static get is() { return "dot-binary-file"; }
    static get properties() { return {
        "accept": {
            "type": String,
            "attr": "accept",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["optionsWatch"]
        },
        "buttonLabel": {
            "type": String,
            "attr": "button-label",
            "reflectToAttr": true
        },
        "clearValue": {
            "method": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "errorMessage": {
            "type": String,
            "attr": "error-message",
            "reflectToAttr": true,
            "mutable": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        },
        "maxFileLength": {
            "type": String,
            "attr": "max-file-length",
            "reflectToAttr": true,
            "mutable": true
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true,
            "mutable": true
        },
        "previewImageName": {
            "type": String,
            "attr": "preview-image-name",
            "reflectToAttr": true,
            "mutable": true
        },
        "previewImageUrl": {
            "type": String,
            "attr": "preview-image-url",
            "reflectToAttr": true,
            "mutable": true
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        },
        "requiredMessage": {
            "type": String,
            "attr": "required-message",
            "watchCallbacks": ["requiredMessageWatch"]
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "URLValidationMessage": {
            "type": String,
            "attr": "u-r-l-validation-message",
            "watchCallbacks": ["URLValidationMessageWatch"]
        },
        "validationMessage": {
            "type": String,
            "attr": "validation-message",
            "watchCallbacks": ["validationMessageWatch"]
        }
    }; }
    static get events() { return [{
            "name": "valueChange",
            "method": "valueChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "statusChange",
            "method": "statusChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get listeners() { return [{
            "name": "fileChange",
            "method": "fileChangeHandler"
        }, {
            "name": "dragover",
            "method": "HandleDragover"
        }, {
            "name": "dragleave",
            "method": "HandleDragleave"
        }, {
            "name": "drop",
            "method": "HandleDrop"
        }, {
            "name": "delete",
            "method": "handleDelete"
        }]; }
    static get style() { return "dot-binary-file.dot-dragover input{background-color:#f1f1f1}dot-binary-file .dot-binary__container button,dot-binary-file .dot-binary__container input{display:-ms-inline-flexbox;display:inline-flex;border:1px solid #d3d3d3;padding:15px;border-radius:0}dot-binary-file .dot-binary__container input[type=file]{display:none}dot-binary-file .dot-binary__container input{min-width:245px;text-overflow:ellipsis}dot-binary-file .dot-binary__container button{background-color:#d3d3d3}"; }
}

class DotBinaryFilePreviewComponent {
    constructor() {
        this.fileName = '';
        this.previewUrl = '';
        this.deleteLabel = 'Delete';
    }
    render() {
        return this.fileName ? (h(Fragment, null,
            this.getPreviewElement(),
            h("div", { class: "dot-file-preview__info" },
                h("span", { class: "dot-file-preview__name" }, this.fileName),
                h("button", { type: "button", onClick: () => this.clearFile() }, this.deleteLabel)))) : null;
    }
    clearFile() {
        this.delete.emit();
        this.fileName = null;
        this.previewUrl = null;
    }
    getPreviewElement() {
        return this.previewUrl ? (h("img", { alt: this.fileName, src: this.previewUrl })) : (h("div", { class: "dot-file-preview__extension" },
            h("span", null, this.getExtention())));
    }
    getExtention() {
        return this.fileName.substr(this.fileName.lastIndexOf('.'));
    }
    static get is() { return "dot-binary-file-preview"; }
    static get properties() { return {
        "deleteLabel": {
            "type": String,
            "attr": "delete-label",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "fileName": {
            "type": String,
            "attr": "file-name",
            "reflectToAttr": true,
            "mutable": true
        },
        "previewUrl": {
            "type": String,
            "attr": "preview-url",
            "reflectToAttr": true,
            "mutable": true
        }
    }; }
    static get events() { return [{
            "name": "delete",
            "method": "delete",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "dot-binary-file-preview{display:-ms-flexbox;display:flex}dot-binary-file-preview .dot-file-preview__extension,dot-binary-file-preview img{width:100px;height:100px}dot-binary-file-preview .dot-file-preview__info{display:-ms-flexbox;display:flex;-ms-flex-direction:column;flex-direction:column;-ms-flex-item-align:end;align-self:flex-end;padding-left:.5rem}dot-binary-file-preview .dot-file-preview__info span{margin-bottom:1rem;word-break:break-all}dot-binary-file-preview .dot-file-preview__info button{-ms-flex-item-align:start;align-self:flex-start;background-color:#d3d3d3;border:0;padding:.3rem 1.5rem;cursor:pointer}dot-binary-file-preview .dot-file-preview__extension{display:-ms-flexbox;display:flex;-ms-flex-align:center;align-items:center;-ms-flex-pack:center;justify-content:center;background-color:#d3d3d3}dot-binary-file-preview .dot-file-preview__extension span{overflow:hidden;padding:.5rem;text-overflow:ellipsis;font-size:2rem}"; }
}

class DotBinaryTextFieldComponent {
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
    static get style() { return ""; }
}

class DotBinaryUploadButtonComponent {
    constructor() {
        this.name = '';
        this.required = false;
        this.disabled = false;
        this.buttonLabel = '';
    }
    componentDidLoad() {
        this.fileInput = this.el.querySelector('dot-label input');
    }
    render() {
        return (h(Fragment, null,
            h("input", { accept: this.accept, disabled: this.disabled, id: getId(this.name), onChange: (event) => this.fileChangeHandler(event), required: this.required || null, type: "file" }),
            h("button", { type: "button", disabled: this.disabled, onClick: () => { this.fileInput.click(); } }, this.buttonLabel)));
    }
    fileChangeHandler(event) {
        const file = this.fileInput.files[0];
        if (isFileAllowed(file.name, this.accept)) {
            this.emitFile(file);
        }
        else {
            event.preventDefault();
            this.emitFile(null, DotBinaryMessageError.INVALID);
        }
    }
    emitFile(file, errorType) {
        this.fileChange.emit({
            file: file,
            errorType: errorType
        });
    }
    static get is() { return "dot-binary-upload-button"; }
    static get properties() { return {
        "accept": {
            "type": String,
            "attr": "accept",
            "reflectToAttr": true
        },
        "buttonLabel": {
            "type": String,
            "attr": "button-label",
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
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        }
    }; }
    static get events() { return [{
            "name": "fileChange",
            "method": "fileChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return ""; }
}

export { DotBinaryFileComponent as DotBinaryFile, DotBinaryFilePreviewComponent as DotBinaryFilePreview, DotBinaryTextFieldComponent as DotBinaryTextField, DotBinaryUploadButtonComponent as DotBinaryUploadButton };
