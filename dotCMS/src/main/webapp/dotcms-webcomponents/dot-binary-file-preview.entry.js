import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';

const DotBinaryFilePreviewComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** file name to be displayed */
        this.fileName = '';
        /** (optional) file URL to be displayed */
        this.previewUrl = '';
        /** (optional) Delete button's label */
        this.deleteLabel = 'Delete';
        this.delete = createEvent(this, "delete", 7);
    }
    render() {
        return this.fileName ? (h(Host, null, this.getPreviewElement(), h("div", { class: "dot-file-preview__info" }, h("span", { class: "dot-file-preview__name" }, this.fileName), h("button", { type: "button", onClick: () => this.clearFile() }, this.deleteLabel)))) : null;
    }
    clearFile() {
        this.delete.emit();
        this.fileName = null;
        this.previewUrl = null;
    }
    getPreviewElement() {
        return this.previewUrl ? (h("img", { alt: this.fileName, src: this.previewUrl })) : (h("div", { class: "dot-file-preview__extension" }, h("span", null, this.getExtention())));
    }
    getExtention() {
        return this.fileName.substr(this.fileName.lastIndexOf('.'));
    }
    get el() { return getElement(this); }
    static get style() { return "dot-binary-file-preview {\n  display: -ms-flexbox;\n  display: flex;\n}\ndot-binary-file-preview img,\ndot-binary-file-preview .dot-file-preview__extension {\n  width: 100px;\n  height: 100px;\n}\ndot-binary-file-preview .dot-file-preview__info {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-direction: column;\n  flex-direction: column;\n  -ms-flex-item-align: end;\n  align-self: flex-end;\n  padding-left: 0.5rem;\n}\ndot-binary-file-preview .dot-file-preview__info span {\n  margin-bottom: 1rem;\n  word-break: break-all;\n}\ndot-binary-file-preview .dot-file-preview__info button {\n  -ms-flex-item-align: start;\n  align-self: flex-start;\n  background-color: lightgray;\n  border: 0;\n  padding: 0.3rem 1.5rem;\n  cursor: pointer;\n}\ndot-binary-file-preview .dot-file-preview__extension {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: center;\n  align-items: center;\n  -ms-flex-pack: center;\n  justify-content: center;\n  background-color: lightgray;\n}\ndot-binary-file-preview .dot-file-preview__extension span {\n  overflow: hidden;\n  padding: 0.5rem;\n  text-overflow: ellipsis;\n  font-size: 2rem;\n}"; }
};

export { DotBinaryFilePreviewComponent as dot_binary_file_preview };
