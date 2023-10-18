import { DotCMSContentType, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

export interface EditContentFormData {
    layout: DotCMSContentTypeLayoutRow[];
    values?: DotCMSContentType;
}
