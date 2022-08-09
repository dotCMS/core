import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field.component';
import { DotTempFileUploadService } from '../../../../../../../apps/dotcms-ui/src/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '../../../../../../../apps/dotcms-ui/src/app/api/services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';

@NgModule({
    imports: [CommonModule],
    declarations: [DotTemplateThumbnailFieldComponent],
    exports: [DotTemplateThumbnailFieldComponent],
    providers: [DotTempFileUploadService, DotWorkflowActionsFireService],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotTemplateThumbnailFieldModule {}
