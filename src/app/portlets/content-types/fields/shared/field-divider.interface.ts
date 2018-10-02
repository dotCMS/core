import { ContentTypeField } from '@portlets/content-types/fields';

export class FieldDivider {
    private fieldDivider: ContentTypeField;

    getFieldDivider(): ContentTypeField {
        return this.fieldDivider;
    }

    setFieldDivider(field: ContentTypeField): void {
        this.fieldDivider = field;
    }
}
