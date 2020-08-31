import Fragment from 'stencil-fragment';
export class DotChipComponent {
    constructor() {
        this.label = '';
        this.deleteLabel = 'Delete';
        this.disabled = false;
    }
    render() {
        const label = this.label ? `${this.deleteLabel} ${this.label}` : null;
        return (h(Fragment, null,
            h("span", null, this.label),
            h("button", { type: "button", "aria-label": label, disabled: this.disabled, onClick: () => this.remove.emit(this.label) }, this.deleteLabel)));
    }
    static get is() { return "dot-chip"; }
    static get properties() { return {
        "deleteLabel": {
            "type": String,
            "attr": "delete-label",
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
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        }
    }; }
    static get events() { return [{
            "name": "remove",
            "method": "remove",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:dot-chip:**/"; }
}
