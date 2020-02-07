import { r as registerInstance, h } from './core-bb6a6489.js';

const DotErrorMessageComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
    }
    render() {
        return h("slot", null);
    }
    static get style() { return "dot-error-message:not(:empty) {\n  border: solid 1px currentColor;\n  color: indianred;\n  display: block;\n  padding: 0.5rem 0.75rem;\n}"; }
};

export { DotErrorMessageComponent as dot_error_message };
