import { DotCMSContentTypeField } from 'dotcms-models';

export interface FieldProperty {
    name: string;
    value: any;
    field: DotCMSContentTypeField;
}
