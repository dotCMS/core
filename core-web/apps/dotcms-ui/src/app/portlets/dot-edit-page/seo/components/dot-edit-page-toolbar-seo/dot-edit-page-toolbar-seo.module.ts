import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotGlobalMessageModule } from '@dotcms/app/view/components/_common/dot-global-message/dot-global-message.module';
import { DotPropertiesService } from '@dotcms/data-access';
import { DotIconModule, UiDotIconButtonModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFavoritePageModule } from '@portlets/dot-edit-page/components/dot-favorite-page/dot-favorite-page.module';
import { DotEditPageStateControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageViewAsControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';

import { DotEditPageToolbarSeoComponent } from './dot-edit-page-toolbar-seo.component';

import { DotEditPageInfoSeoModule } from '../dot-edit-page-info-seo/dot-edit-page-info-seo.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        CheckboxModule,
        DotEditPageWorkflowsActionsModule,
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
        DotEditPageNavDirective,
        RouterLink,
        TagModule,
        DotEditPageInfoSeoModule
    ],
    exports: [DotEditPageToolbarSeoComponent],
    declarations: [DotEditPageToolbarSeoComponent],
    providers: [DialogService, DotPropertiesService]
})
export class DotEditPageToolbarSeoModule {}
