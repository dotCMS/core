import { r as registerInstance, h, H as Host } from './core-5e49af37.js';

const DotBadge = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.bgColor = null;
        this.color = null;
        this.size = null;
    }
    render() {
        return (h(Host, { style: {
                '--bg-color': this.bgColor,
                '--color': this.color,
                '--font-size': this.size
            } }, h("slot", null)));
    }
    static get style() { return ":host {\n  --bg-color: var(--color-sec);\n  --color: var(--color-white);\n  --font-size: 12px;\n  background-color: var(--bg-color);\n  border-radius: var(--border-radius);\n  color: var(--color);\n  font-size: var(--font-size);\n  padding: 0.25em 0.5em;\n}"; }
};

export { DotBadge as dot_badge };
