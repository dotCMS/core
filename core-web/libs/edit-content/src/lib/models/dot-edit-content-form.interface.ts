import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab,
    DotCMSContentlet
} from '@dotcms/dotcms-models';

export interface EditContentFormData {
    layout: DotCMSContentTypeLayoutRow[];
    fields: DotCMSContentTypeField[];
    tabs: DotCMSContentTypeLayoutTab[];
    contentlet?: DotCMSContentlet;
}
