import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { FormsModule } from '@angular/forms';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ToolbarModule } from 'primeng/toolbar';
import { DotGlobalMessageModule } from '@dotcms/app/view/components/_common/dot-global-message/dot-global-message.module';
import { DotFavoritePageModule } from '../../../components/dot-favorite-page/dot-favorite-page.module';
import { DialogService } from 'primeng/dynamicdialog';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { TooltipModule } from 'primeng/tooltip';
import { DotPropertiesService } from '@dotcms/data-access';

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
        FormsModule,
        ToolbarModule,
        TooltipModule,
        DotPipesModule,
        DotGlobalMessageModule,
        DotFavoritePageModule,
        UiDotIconButtonModule
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent],
    providers: [DialogService, DotPropertiesService]
})
export class DotEditPageToolbarModule {}
