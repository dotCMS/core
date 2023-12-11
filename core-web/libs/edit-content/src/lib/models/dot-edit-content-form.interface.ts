import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

export interface EditContentFormData {
    layout: DotCMSContentTypeLayoutRow[];
    fields: DotCMSContentTypeField[];
    contentlet?: DotCMSContentlet;
    contentType: string;
}
