import { ContentTypeField } from './field.model';
import { FieldUtil } from '../util/field-util';

export class FieldColumn {
    fields: ContentTypeField[];
    columnDivider: ContentTypeField;
    private ngId: string;

    constructor(fields: ContentTypeField[] = []) {
        if (fields.length && FieldUtil.isColumn(fields[0])) {
            this.columnDivider = fields[0];
            this.fields = fields.splice(1);
        } else {
            this.columnDivider = FieldUtil.createFieldColumn();
            this.ngId = FieldUtil.createNGID();
            this.fields = fields;
        }
    }

    get id(): string{
        return this.columnDivider.id || this.ngId;
    }
}
