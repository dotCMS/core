import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';
import { h as getOriginalStatus, c as checkProp, a as getClassNames, e as getTagHint, f as getTagError, u as updateStatus, m as isFileAllowed } from './index-fca8faa0.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-b2e001f1.js';
import { D as DotBinaryMessageError } from './index-2f031ed5.js';

const DotBinaryFileComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
        this.placeholder = 'Drop or paste a file or url';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Text that be shown when required is set and condition not met */
        this.requiredMessage = 'This field is required';
        /** (optional) Text that be shown when the Regular Expression condition not met */
        this.validationMessage = "The field doesn't comply with the specified format";
        /** (optional) Text that be shown when the URL is not valid */
        this.URLValidationMessage = 'The specified URL is not valid';
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Describes a type of file that may be selected by the user, separated by comma  eg: .pdf,.jpg  */
        this.accept = '';
        /** (optional) Set the max file size limit  */
        this.maxFileLength = '';
        /** (optional) Text that be shown in the browse file button */
        this.buttonLabel = 'Browse';
        /** (optional) Text that be shown in the browse file button */
        this.errorMessage = '';
        /** (optional) Name of the file uploaded */
        this.previewImageName = '';
        /** (optional) URL of the file uploaded */
        this.previewImageUrl = '';
        this.file = null;
        this.allowedFileTypes = [];
        this.errorMessageMap = new Map();
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     */
    async reset() {
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
    async clearValue() {
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
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name, tabindex: "0" }, this.previewImageName ? (h("dot-binary-file-preview", { onClick: (e) => {
                e.preventDefault();
            }, fileName: this.previewImageName, previewUrl: this.previewImageUrl })) : (h("div", { class: "dot-binary__container" }, h("dot-binary-text-field", { placeholder: this.placeholder, required: this.required, disabled: this.disabled, accept: this.allowedFileTypes.join(','), hint: this.hint, onLostFocus: this.lostFocusEventHandler.bind(this) }), h("dot-binary-upload-button", { name: this.name, accept: this.allowedFileTypes.join(','), disabled: this.disabled, required: this.required, buttonLabel: this.buttonLabel })))), getTagHint(this.hint), getTagError(this.shouldShowErrorMessage(), this.getErrorMessage()), h("dot-error-message", null, this.errorMessage)));
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
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.dotValueChange.emit({
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
    get el() { return getElement(this); }
    static get watchers() { return {
        "requiredMessage": ["requiredMessageWatch"],
        "validationMessage": ["validationMessageWatch"],
        "URLValidationMessage": ["URLValidationMessageWatch"],
        "accept": ["optionsWatch"]
    }; }
    static get style() { return "dot-binary-file.dot-dragover input {\n  background-color: #f1f1f1;\n}\ndot-binary-file .dot-binary__container input,\ndot-binary-file .dot-binary__container button {\n  display: -ms-inline-flexbox;\n  display: inline-flex;\n  border: 1px solid lightgray;\n  padding: 15px;\n  border-radius: 0;\n}\ndot-binary-file .dot-binary__container input[type=file] {\n  display: none;\n}\ndot-binary-file .dot-binary__container input {\n  min-width: 245px;\n  text-overflow: ellipsis;\n}\ndot-binary-file .dot-binary__container button {\n  background-color: lightgray;\n}"; }
};

export { DotBinaryFileComponent as dot_binary_file };
