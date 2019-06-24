import { DotContentTypeField } from './dot-content-type-field.model';

export interface DotContentTypeColumn {
    fields: DotContentTypeField[];
    columnDivider: DotContentTypeField;
}
