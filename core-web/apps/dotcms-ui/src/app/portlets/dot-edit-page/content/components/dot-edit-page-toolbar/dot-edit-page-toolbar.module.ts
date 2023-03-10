import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotGlobalMessageModule } from '@dotcms/app/view/components/_common/dot-global-message/dot-global-message.module';
import { DotPropertiesService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';

import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';

import { DotFavoritePageModule } from '../../../components/dot-favorite-page/dot-favorite-page.module';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';

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
        UiDotIconButtonModule,
        DotIconModule,
        DotEditPageNavDirective
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent],
    providers: [DialogService, DotPropertiesService]
})
export class DotEditPageToolbarModule {}
