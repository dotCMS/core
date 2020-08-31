import Fragment from 'stencil-fragment';
import { DotBinaryMessageError } from '../../models';
import { checkProp, getClassNames, getOriginalStatus, getTagError, getTagHint, isFileAllowed, updateStatus } from '../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';
export class DotBinaryFileComponent {
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
    static get style() { return "/**style-placeholder:dot-binary-file:**/"; }
}
