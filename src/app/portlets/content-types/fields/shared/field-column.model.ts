import { ContentTypeField } from './field.model';
import { FieldUtil } from '../util/field-util';

export class FieldColumn {
    fields: ContentTypeField[];
    tabDivider: ContentTypeField;

    constructor(fields: ContentTypeField[] = []) {
        if (fields.length && FieldUtil.isColumn(fields[0])) {
            this.tabDivider = fields[0];
            this.fields = fields.splice(1);
        } else {
            this.tabDivider = FieldUtil.createTabDivider();
            this.fields = fields;
        }
    }
}
