import { r as registerInstance, h, H as Host } from './core-bb6a6489.js';

const DotContentletThumbnail = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.height = '';
        this.width = '';
        this.alt = '';
        this.iconSize = '';
    }
    componentWillLoad() {
        var _a;
        this.renderImage = ((_a = this.contentlet) === null || _a === void 0 ? void 0 : _a.hasTitleImage) === 'true';
    }
    render() {
        var _a;
        return (h(Host, null, this.renderImage ? (h("img", { src: this.getImageURL(), alt: this.alt, "aria-label": this.alt, onError: () => {
                this.switchToIcon();
            } })) : (h("dot-contentlet-icon", { icon: (_a = this.contentlet) === null || _a === void 0 ? void 0 : _a.__icon__, size: this.iconSize, "aria-label": this.alt }))));
    }
    getImageURL() {
        return `/dA/${this.contentlet.identifier}/${this.width}`;
    }
    switchToIcon() {
        this.renderImage = false;
    }
    static get style() { return ":host {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: center;\n  align-items: center;\n  -ms-flex: 1;\n  flex: 1;\n}\n\ndot-contentlet-icon {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: center;\n  align-items: center;\n  -ms-flex-pack: center;\n  justify-content: center;\n  width: 100%;\n  height: 100%;\n}\n\nimg {\n  max-width: 100%;\n  max-height: 100%;\n}"; }
};

export { DotContentletThumbnail as dot_contentlet_thumbnail };
