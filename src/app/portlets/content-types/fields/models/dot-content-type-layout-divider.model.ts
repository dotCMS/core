import { DotContentTypeField, DotContentTypeColumn } from '@portlets/content-types/fields';

export interface DotContentTypeLayoutDivider {
    divider: DotContentTypeField;
    columns?: DotContentTypeColumn[];
}
