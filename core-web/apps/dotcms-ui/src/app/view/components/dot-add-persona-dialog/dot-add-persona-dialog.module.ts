import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotCreatePersonaFormModule } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.module';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotDialogModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

@NgModule({
    imports: [
        CommonModule,
        DotCreatePersonaFormModule,
        DotDialogModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    providers: [DotWorkflowActionsFireService],
    declarations: [DotAddPersonaDialogComponent],
    exports: [DotAddPersonaDialogComponent]
})
export class DotAddPersonaDialogModule {}
