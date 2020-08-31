import { getLabelId } from '../../utils';
export class DotLabelComponent {
    constructor() {
        this.name = '';
        this.label = '';
        this.required = false;
    }
    render() {
        return (h("label", { class: "dot-label", id: getLabelId(this.name) },
            h("span", { class: "dot-label__text" },
                this.label,
                this.required ? h("span", { class: "dot-label__required-mark" }, "*") : null),
            h("slot", null)));
    }
    static get is() { return "dot-label"; }
    static get properties() { return {
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
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
    static get style() { return "/**style-placeholder:dot-label:**/"; }
}
