import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWizardComponent } from '@components/_common/dot-wizard/dot-wizard.component';
import { DotCommentAndAssignFormModule } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.module';
import { DotPushPublishFormModule } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotContainerReferenceModule } from '@directives/dot-container-reference/dot-container-reference.module';
import { DotWizardService } from '@dotcms/data-access';
import { DotSafeHtmlPipe } from '@dotcms/ui';

@NgModule({
    imports: [
        CommonModule,
        DotSafeHtmlPipe,
        DotCommentAndAssignFormModule,
        DotPushPublishFormModule,
        DotDialogModule,
        DotContainerReferenceModule
    ],
    declarations: [DotWizardComponent],
    exports: [DotWizardComponent],
    providers: [DotWizardService]
})
export class DotWizardModule {}
