import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { ButtonModule, MenuModule } from 'primeng/primeng';
import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';

@NgModule({
    imports: [CommonModule, ButtonModule, MenuModule],
    exports: [DotEditPageWorkflowsActionsComponent],
    declarations: [DotEditPageWorkflowsActionsComponent],
    providers: [DotWorkflowsActionsService]
})
export class DotEditPageWorkflowsActionsModule {}
