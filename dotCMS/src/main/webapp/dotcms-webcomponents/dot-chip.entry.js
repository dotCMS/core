import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';

const DotChipComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Chip's label */
        this.label = '';
        /** (optional) Delete button's label */
        this.deleteLabel = 'Delete';
        /** (optional) If is true disabled the delete button */
        this.disabled = false;
        this.remove = createEvent(this, "remove", 7);
    }
    render() {
        const label = this.label ? `${this.deleteLabel} ${this.label}` : null;
        return (h(Host, null, h("span", null, this.label), h("button", { type: "button", "aria-label": label, disabled: this.disabled, onClick: () => this.remove.emit(this.label) }, this.deleteLabel)));
    }
    get el() { return getElement(this); }
    static get style() { return "dot-chip span {\n  margin-right: 0.25rem;\n}\ndot-chip button {\n  cursor: pointer;\n}"; }
};

export { DotChipComponent as dot_chip };
