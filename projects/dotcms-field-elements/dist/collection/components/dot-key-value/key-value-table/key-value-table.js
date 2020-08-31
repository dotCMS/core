export class KeyValueTableComponent {
    constructor() {
        this.items = [];
        this.disabled = false;
        this.buttonLabel = 'Delete';
        this.emptyMessage = 'No values';
    }
    render() {
        return (h("table", null,
            h("tbody", null, this.renderRows(this.items))));
    }
    onDelete(index) {
        this.delete.emit(index);
    }
    getRow(item, index) {
        const label = `${this.buttonLabel} ${item.key}, ${item.value}`;
        return (h("tr", null,
            h("td", null,
                h("button", { "aria-label": label, disabled: this.disabled || null, onClick: () => this.onDelete(index), class: "dot-key-value__delete-button" }, this.buttonLabel)),
            h("td", null, item.key),
            h("td", null, item.value)));
    }
    renderRows(items) {
        return this.isValidItems(items) ? items.map(this.getRow.bind(this)) : this.getEmptyRow();
    }
    getEmptyRow() {
        return (h("tr", null,
            h("td", null, this.emptyMessage)));
    }
    isValidItems(items) {
        return Array.isArray(items) && !!items.length;
    }
    static get is() { return "key-value-table"; }
    static get properties() { return {
        "buttonLabel": {
            "type": String,
            "attr": "button-label",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "emptyMessage": {
            "type": String,
            "attr": "empty-message",
            "reflectToAttr": true
        },
        "items": {
            "type": "Any",
            "attr": "items"
        }
    }; }
    static get events() { return [{
            "name": "delete",
            "method": "delete",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
}
