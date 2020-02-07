import { r as registerInstance, h } from './core-bb6a6489.js';

const DotCard = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
    }
    render() {
        return (h("slot", null));
    }
    static get style() { return ":host {\n  background-color: var(--color-white);\n  border-radius: var(--border-radius);\n  -webkit-box-shadow: var(--md-shadow-1);\n  box-shadow: var(--md-shadow-1);\n  display: block;\n}"; }
};

export { DotCard as dot_card };
