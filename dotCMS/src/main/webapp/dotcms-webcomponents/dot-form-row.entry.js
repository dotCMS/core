import { r as registerInstance, h } from './core-bb6a6489.js';

const DotFormRowComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
    }
    render() {
        return this.row.columns.map((fieldColumn) => {
            return h("dot-form-column", { column: fieldColumn, "fields-to-show": this.fieldsToShow });
        });
    }
    static get style() { return "dot-form-row {\n  display: -ms-flexbox;\n  display: flex;\n}"; }
};

export { DotFormRowComponent as dot_form_row };
