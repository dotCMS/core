import { HttpResponse } from '@angular/common/http';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

/**
 * Interface used for the response of FileUpload of primeng
 * https://primeng.org/fileupload/#api.fileupload.events.FileUploadEvent
 *
 * @interface
 */
export interface DotFileUpload {
    originalEvent: HttpResponse<DotFileUploadEvent>;
    files: File[];
}

export interface DotFileUploadEvent {
    tempFiles: File[] | DotCMSTempFile[];
}
