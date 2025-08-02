import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotDialogModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAddPersonaDialogComponent } from './dot-add-persona-dialog.component';
import { DotCreatePersonaFormModule } from './dot-create-persona-form/dot-create-persona-form.module';

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
