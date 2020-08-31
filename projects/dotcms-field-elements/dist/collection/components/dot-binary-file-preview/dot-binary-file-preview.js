import Fragment from 'stencil-fragment';
export class DotBinaryFilePreviewComponent {
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
    static get style() { return "/**style-placeholder:dot-binary-file-preview:**/"; }
}
