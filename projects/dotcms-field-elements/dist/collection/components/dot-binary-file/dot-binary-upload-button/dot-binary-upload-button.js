import Fragment from 'stencil-fragment';
import { DotBinaryMessageError } from '../../../models';
import { getId, isFileAllowed } from '../../../utils';
export class DotBinaryUploadButtonComponent {
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
    static get style() { return "/**style-placeholder:dot-binary-upload-button:**/"; }
}
