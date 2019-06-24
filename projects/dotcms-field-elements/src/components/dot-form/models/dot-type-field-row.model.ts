import { DotCMSContentTypeColumn, DotCMSContentTypeField } from '.';

export interface DotCMSContentTypeRow {
    columns: DotCMSContentTypeColumn[];
    divider?: DotCMSContentTypeField;
}
