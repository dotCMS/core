export class DotFormRowComponent {
    render() {
        return this.row.columns.map((fieldColumn) => {
            return h("dot-form-column", { column: fieldColumn, "fields-to-show": this.fieldsToShow });
        });
    }
    static get is() { return "dot-form-row"; }
    static get properties() { return {
        "fieldsToShow": {
            "type": String,
            "attr": "fields-to-show",
            "reflectToAttr": true
        },
        "row": {
            "type": "Any",
            "attr": "row"
        }
    }; }
    static get style() { return "/**style-placeholder:dot-form-row:**/"; }
}
