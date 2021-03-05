import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotWizardComponent } from '@components/_common/dot-wizard/dot-wizard.component';
import { DotCommentAndAssignFormModule } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.module';
import { DotPushPublishFormModule } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotContainerReferenceModule } from '@directives/dot-container-reference/dot-container-reference.module';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';

@NgModule({
    imports: [
        CommonModule,
        DotPipesModule,
        DotCommentAndAssignFormModule,
        DotPushPublishFormModule,
        DotDialogModule,
        DotContainerReferenceModule
    ],
    declarations: [DotWizardComponent],
    exports: [DotWizardComponent],
    entryComponents: [DotCommentAndAssignFormComponent, DotPushPublishFormComponent],
    providers: [DotWizardService]
})
export class DotWizardModule {}
