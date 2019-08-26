import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAddPersonaDialogComponent } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.component';
import { DotCreatePersonaFormModule } from '@components/dot-add-persona-dialog/dot-create-persona-form/dot-create-persona-form.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';

@NgModule({
    imports: [CommonModule, DotCreatePersonaFormModule, DotDialogModule],
    providers: [DotWorkflowActionsFireService],
    declarations: [DotAddPersonaDialogComponent],
    exports: [DotAddPersonaDialogComponent]
})
export class DotAddPersonaDialogModule {}
