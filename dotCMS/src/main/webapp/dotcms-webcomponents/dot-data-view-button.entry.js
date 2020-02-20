import { r as registerInstance, h } from './core-5e49af37.js';

const DotDataViewButton = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
    }
    render() {
        return (h("dot-select-button", { value: this.value, options: [
                { label: 'Card', icon: 'grid_on' },
                { label: 'List', icon: 'format_list_bulleted' }
            ] }));
    }
    static get style() { return "dot-data-view-button {\n    /* Component styles go here */\n}"; }
};

export { DotDataViewButton as dot_data_view_button };
