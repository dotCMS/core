import { r as registerInstance, c as createEvent, h, H as Host } from './core-5e49af37.js';

const getValueAsArray = (value) => {
    return value && typeof value === 'string' ? value.split(',') : [];
};
const getSelecttion = (items, value) => {
    if (items && items.length && value && typeof value === 'string') {
        return items
            .filter(({ data: { inode } }) => value.split(',').includes(inode))
            .map(({ data }) => data);
    }
    return [];
};
const DotCardView = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        this.items = [];
        this.selection = [];
        this.selected = createEvent(this, "selected", 7);
        this.onCardClick = createEvent(this, "onCardClick", 7);
    }
    async getValue() {
        return this.selection;
    }
    watchItems(newValue) {
        this.selection = getSelecttion(newValue, this.value);
    }
    watchValue(newValue) {
        this.selection = getSelecttion(this.items, newValue);
    }
    render() {
        const value = getValueAsArray(this.value);
        return (h(Host, null, this.items.map((item) => (h("dot-card-contentlet", { onClick: () => {
                this.onCardClick.emit(item.data);
            }, key: item.data.inode, checked: value.includes(item.data.inode), onCheckboxChange: ({ detail: { checked, data } }) => {
                if (checked) {
                    this.selection.push(data);
                }
                else {
                    this.selection = this.selection.filter((item) => item.identifier !== data.identifier);
                }
                this.value = this.selection
                    .map(({ inode }) => inode)
                    .join(',');
                this.selected.emit(this.selection);
            }, item: item })))));
    }
    static get watchers() { return {
        "items": ["watchItems"],
        "value": ["watchValue"]
    }; }
    static get style() { return ":host {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));\n  grid-gap: var(--basic-padding-2);\n}\n\ndot-card-contentlet {\n  height: 100%;\n}\ndot-card-contentlet:before {\n  content: \"\";\n  display: inline-block;\n  -ms-flex: 0 0 0px;\n  flex: 0 0 0;\n  height: 0;\n  padding-bottom: calc(100%);\n}"; }
};

export { DotCardView as dot_card_view };
