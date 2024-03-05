import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWizardService } from '@dotcms/data-access';

import { DotWizardComponent } from './dot-wizard.component';

import { DotContainerReferenceModule } from '../../../directives/dot-container-reference/dot-container-reference.module';
import { DotPipesModule } from '../../../pipes/dot-pipes.module';
import { DotDialogModule } from '../../dot-dialog/dot-dialog.module';
import { DotCommentAndAssignFormModule } from '../forms/dot-comment-and-assign-form/dot-comment-and-assign-form.module';
import { DotPushPublishFormModule } from '../forms/dot-push-publish-form/dot-push-publish-form.module';

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
    providers: [DotWizardService]
})
export class DotWizardModule {}
