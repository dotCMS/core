import { r as registerInstance, h, H as Host } from './core-bb6a6489.js';

const DotCardView = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.items = [];
    }
    render() {
        return (h(Host, null, this.items.map((item) => (h("dot-card-contentlet", { item: item })))));
    }
    static get style() { return ":host {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));\n  grid-gap: var(--basic-padding-2);\n}\n\ndot-card-contentlet:before {\n  content: \"\";\n  display: inline-block;\n  -ms-flex: 0 0 0px;\n  flex: 0 0 0;\n  height: 0;\n  padding-bottom: calc(100%);\n}"; }
};

export { DotCardView as dot_card_view };
