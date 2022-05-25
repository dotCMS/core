import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotTempFileUploadService } from '@services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotTemplateThumbnailFieldComponent],
    exports: [DotTemplateThumbnailFieldComponent],
    providers: [DotTempFileUploadService, DotWorkflowActionsFireService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotTemplateThumbnailFieldModule {}
