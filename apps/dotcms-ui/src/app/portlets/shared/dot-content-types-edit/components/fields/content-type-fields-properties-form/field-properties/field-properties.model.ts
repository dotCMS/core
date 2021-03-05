import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

export interface FieldProperty {
    name: string;
    value: any;
    field: DotCMSContentTypeField;
}
