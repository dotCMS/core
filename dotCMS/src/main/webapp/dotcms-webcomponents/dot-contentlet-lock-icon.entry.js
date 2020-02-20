import { r as registerInstance, h, H as Host } from './core-5e49af37.js';
import './lit-element-aeb3818e.js';
import './mwc-icon-0d1aac57.js';

const DotContentletLockIcon = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.size = '16px';
    }
    render() {
        return (h(Host, { style: { '--mdc-icon-size': this.size } }, h("mwc-icon", null, this.contentlet.locked === 'true' ? 'locked' : 'lock_open')));
    }
    static get style() { return ":host {\n  width: var(--mdc-icon-size);\n  height: var(--mdc-icon-size);\n}"; }
};

export { DotContentletLockIcon as dot_contentlet_lock_icon };
