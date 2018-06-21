import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { ToolbarModule, SelectButtonModule, InputSwitchModule, ButtonModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        DotEditPageWorkflowsActionsModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        ToolbarModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent, DotEditPageLockInfoComponent]
})
export class DotEditPageToolbarModule {}
