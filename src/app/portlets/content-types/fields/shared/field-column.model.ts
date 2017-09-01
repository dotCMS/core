import { Field } from './field.model';
import { FieldUtil } from '../util/field-util';

export class FieldColumn {
    fields: Field[];
    tabDivider: Field;

    constructor(fields: Field[] = []) {
        if (fields.length && FieldUtil.isColumn(fields[0])) {
            this.tabDivider = fields[0];
            this.fields = fields.splice(1);
        } else {
            this.tabDivider = FieldUtil.createTabDivider();
            this.fields = fields;
        }
    }
}
