import { Field } from './field';
import { TAB_DIVIDER } from './';

export class FieldColumn {
    fields: Field[];
    tabDivider: Field;

    constructor(fields: Field[] = []) {
        if (fields.length && fields[0].clazz === TAB_DIVIDER.clazz) {
            this.tabDivider = fields[0];
            this.fields = fields.splice(1);
        } else {
            this.tabDivider = Object.assign({}, TAB_DIVIDER);
            this.fields = fields;
        }
    }
}