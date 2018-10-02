import { FieldDivider } from '@portlets/content-types/fields/shared/field-divider.interface';
import { ContentTypeField } from '@portlets/content-types/fields/shared';

export class FieldTab extends FieldDivider {

    constructor(fieldTab: ContentTypeField) {
        super();
        this.setFieldDivider(fieldTab);
    }
}
