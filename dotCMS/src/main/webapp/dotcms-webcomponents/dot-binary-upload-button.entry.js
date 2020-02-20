import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { a as getId, l as isFileAllowed } from './index-d52678cd.js';
import { D as DotBinaryMessageError } from './index-2f031ed5.js';

const DotBinaryUploadButtonComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Text that be shown in the browse file button */
        this.buttonLabel = '';
        this.fileChange = createEvent(this, "fileChange", 7);
    }
    componentDidLoad() {
        this.fileInput = this.el.querySelector('dot-label input');
    }
    render() {
        return (h(Host, null, h("input", { accept: this.accept, disabled: this.disabled, id: getId(this.name), onChange: (event) => this.fileChangeHandler(event), required: this.required || null, type: "file" }), h("button", { type: "button", disabled: this.disabled, onClick: () => {
                this.fileInput.click();
            } }, this.buttonLabel)));
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
    get el() { return getElement(this); }
    static get style() { return ""; }
};

export { DotBinaryUploadButtonComponent as dot_binary_upload_button };
