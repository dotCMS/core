import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { CheckboxModule, ToolbarModule, ButtonModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';
import { DotGlobalMessageModule } from '@components/_common/dot-global-message/dot-global-message.module';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        CheckboxModule,
        DotEditPageWorkflowsActionsModule,
        DotEditPageInfoModule,
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        DotSecondaryToolbarModule,
        DotGlobalMessageModule,
        FormsModule,
        ToolbarModule,
        DotPipesModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent]
})
export class DotEditPageToolbarModule {}
