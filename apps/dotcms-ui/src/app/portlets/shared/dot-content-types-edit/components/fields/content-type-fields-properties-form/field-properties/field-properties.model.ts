import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

export interface FieldProperty<T = unknown> {
    name: string;
    value: T;
    field: DotCMSContentTypeField;
}
