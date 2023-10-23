import { DotCMSContentTypeLayoutRow, DotCMSContentlet } from '@dotcms/dotcms-models';

export interface EditContentFormData {
    layout: DotCMSContentTypeLayoutRow[];
    contentlet?: DotCMSContentlet;
}
