import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotCreatePersonaFormModule } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DotCreatePersonaFormModule,
        DotDialogModule,
        DotPipesModule,
        DotMessagePipe
    ],
    providers: [DotWorkflowActionsFireService],
    declarations: [DotAddPersonaDialogComponent],
    exports: [DotAddPersonaDialogComponent]
})
export class DotAddPersonaDialogModule {}
