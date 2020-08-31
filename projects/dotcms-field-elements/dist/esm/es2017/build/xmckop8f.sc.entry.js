import { h } from '../dotcmsfields.core.js';

class DotErrorMessageComponent {
    render() {
        return h("slot", null);
    }
    static get is() { return "dot-error-message"; }
    static get style() { return "dot-error-message:not(:empty){border:1px solid;color:#cd5c5c;display:block;padding:.5rem .75rem}"; }
}

export { DotErrorMessageComponent as DotErrorMessage };
