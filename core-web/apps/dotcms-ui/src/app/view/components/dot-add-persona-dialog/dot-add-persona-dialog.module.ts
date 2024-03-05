import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAddPersonaDialogComponent } from './dot-add-persona-dialog.component';
import { DotCreatePersonaFormModule } from './dot-create-persona-form/dot-create-persona-form.module';

import { DotPipesModule } from '../../pipes/dot-pipes.module';
import { DotDialogModule } from '../dot-dialog/dot-dialog.module';

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
