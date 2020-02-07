import { r as registerInstance, h } from './core-bb6a6489.js';
import { o as getLabelId } from './index-fca8faa0.js';

const DotLabelComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** (optional) Field name */
        this.name = '';
        /** (optional) Text to be rendered */
        this.label = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
    }
    render() {
        return (h("label", { class: "dot-label", id: getLabelId(this.name) }, h("span", { class: "dot-label__text" }, this.label, this.required ? h("span", { class: "dot-label__required-mark" }, "*") : null), h("slot", null)));
    }
    static get style() { return ".dot-field__error-message,\n.dot-field__hint {\n  display: block;\n  font-size: 0.75rem;\n  line-height: 1rem;\n  margin-top: 0.25rem;\n  position: absolute;\n  -webkit-transition: opacity 200ms ease;\n  transition: opacity 200ms ease;\n}\n\n.dot-field__error-message {\n  color: red;\n  opacity: 0;\n}\n\n.dot-invalid.dot-dirty > .dot-field__hint {\n  opacity: 0;\n}\n.dot-invalid.dot-dirty > .dot-field__error-message {\n  color: red;\n  opacity: 1;\n}\n\ndot-label > label {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-direction: column;\n  flex-direction: column;\n}\ndot-label > label .dot-label__text {\n  line-height: 1.25rem;\n  margin-bottom: 0.25rem;\n}"; }
};

export { DotLabelComponent as dot_label };
