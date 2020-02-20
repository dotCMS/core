import { r as registerInstance, c as createEvent, h } from './core-5e49af37.js';

const KeyValueTableComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** (optional) Items to render in the list of key value */
        this.items = [];
        /** (optional) Disables all form interaction */
        this.disabled = false;
        /** (optional) Label for the delete button in each item list */
        this.buttonLabel = 'Delete';
        /** (optional) Message to show when the list of items is empty */
        this.emptyMessage = 'No values';
        this.delete = createEvent(this, "delete", 7);
    }
    render() {
        return (h("table", null, h("tbody", null, this.renderRows(this.items))));
    }
    onDelete(index) {
        this.delete.emit(index);
    }
    getRow(item, index) {
        const label = `${this.buttonLabel} ${item.key}, ${item.value}`;
        return (h("tr", null, h("td", null, h("button", { "aria-label": label, disabled: this.disabled || null, onClick: () => this.onDelete(index), class: "dot-key-value__delete-button" }, this.buttonLabel)), h("td", null, item.key), h("td", null, item.value)));
    }
    renderRows(items) {
        return this.isValidItems(items)
            ? items.map((item, index) => this.getRow(item, index))
            : this.getEmptyRow();
    }
    getEmptyRow() {
        return (h("tr", null, h("td", null, this.emptyMessage)));
    }
    isValidItems(items) {
        return Array.isArray(items) && !!items.length;
    }
};

export { KeyValueTableComponent as key_value_table };
