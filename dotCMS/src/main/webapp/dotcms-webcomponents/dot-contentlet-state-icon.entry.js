import { r as registerInstance, h, H as Host } from './core-5e49af37.js';

const DotContentletStateIcon = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.size = '16px';
    }
    render() {
        const className = this.getType(this.contentlet);
        return (h(Host, { style: {
                '--size': this.size
            } }, h("div", { class: className })));
    }
    getType({ live, working, deleted, hasLiveVersion }) {
        if (deleted === 'true') {
            return 'archived'; // crossed
        }
        if (live === 'true') {
            if (hasLiveVersion === 'true' && working === 'true') {
                return 'published'; // full
            }
        }
        else {
            if (hasLiveVersion === 'true') {
                return 'drafted'; // half
            }
        }
        return 'draft'; // empty
    }
    static get style() { return ":host {\n  display: -ms-flexbox;\n  display: flex;\n  --sucess-color: #27b970;\n}\n\ndiv {\n  border-radius: 50%;\n  border: solid 2px;\n  -webkit-box-sizing: border-box;\n  box-sizing: border-box;\n  height: var(--size);\n  width: var(--size);\n}\n\n.published,\n.drafted:after {\n  background-color: var(--sucess-color);\n}\n\n.archived,\n.drafted {\n  position: relative;\n}\n.archived:before,\n.drafted:before {\n  -webkit-box-sizing: border-box;\n  box-sizing: border-box;\n  background-color: currentColor;\n  content: \"\";\n  height: 2px;\n  position: absolute;\n  top: 50%;\n  -webkit-transform: translateY(-50%);\n  transform: translateY(-50%);\n  width: calc(var(--size) - 2px);\n  z-index: 1;\n}\n\n.drafted {\n  -webkit-transform: rotate(90deg);\n  transform: rotate(90deg);\n}\n.drafted:after {\n  border-bottom-left-radius: var(--size);\n  border-top-left-radius: var(--size);\n  bottom: 25%;\n  content: \"\";\n  height: 100%;\n  left: 25%;\n  position: absolute;\n  -webkit-transform: rotate(90deg);\n  transform: rotate(90deg);\n  width: 50%;\n}"; }
};

export { DotContentletStateIcon as dot_contentlet_state_icon };
