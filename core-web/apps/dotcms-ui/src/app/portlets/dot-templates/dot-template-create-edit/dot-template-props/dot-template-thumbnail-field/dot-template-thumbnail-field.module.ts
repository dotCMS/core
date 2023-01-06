import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field.component';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';

@NgModule({
    imports: [CommonModule, DotMessagePipeModule],
    declarations: [DotTemplateThumbnailFieldComponent],
    exports: [DotTemplateThumbnailFieldComponent],
    providers: [DotTempFileUploadService, DotWorkflowActionsFireService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotTemplateThumbnailFieldModule {}
