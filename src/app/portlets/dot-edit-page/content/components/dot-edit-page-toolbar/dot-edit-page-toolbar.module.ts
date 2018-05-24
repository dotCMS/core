import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { ToolbarModule, SelectButtonModule, InputSwitchModule, ButtonModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotEditPageInfoModule } from '../../../components/dot-edit-page-info/dot-edit-page-info.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotEditPageInfoModule,
        DotEditPageWorkflowsActionsModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        ToolbarModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent]
})
export class DotEditPageToolbarModule {}
