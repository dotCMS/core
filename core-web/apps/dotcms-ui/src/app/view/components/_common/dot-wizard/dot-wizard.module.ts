import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotWizardService } from '@dotcms/data-access';
import { DotDialogModule, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotWizardComponent } from './dot-wizard.component';

import { DotContainerReferenceModule } from '../../../directives/dot-container-reference/dot-container-reference.module';
import { DotCommentAndAssignFormModule } from '../forms/dot-comment-and-assign-form/dot-comment-and-assign-form.module';
import { DotPushPublishFormModule } from '../forms/dot-push-publish-form/dot-push-publish-form.module';

/**
 * Show a Dialog with a wizard with differents steps
 */
@NgModule({
    imports: [
        CommonModule,
        DialogModule,
        ButtonModule,
        DotPushPublishFormModule,
        DotCommentAndAssignFormModule,
        DotContainerReferenceModule,
        DotSafeHtmlPipe,
        DotDialogModule
    ],
    declarations: [DotWizardComponent],
    exports: [DotWizardComponent],
    providers: [DotWizardService]
})
export class DotWizardModule {}
