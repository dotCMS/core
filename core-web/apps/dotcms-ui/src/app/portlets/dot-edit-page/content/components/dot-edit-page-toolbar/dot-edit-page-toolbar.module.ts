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

import { DotPropertiesService } from '@dotcms/data-access';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';

import { DotGlobalMessageComponent } from '../../../../../view/components/_common/dot-global-message/dot-global-message.component';
import { DotSecondaryToolbarComponent } from '../../../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotEditPageInfoComponent } from '../../../components/dot-edit-page-info/dot-edit-page-info.component';
import { DotEditPageNavDirective } from '../../../main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';

@NgModule({
    imports: [
        ButtonModule,
        CommonModule,
        CheckboxModule,
        DotEditPageWorkflowsActionsModule,
        DotEditPageInfoComponent,
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        DotSecondaryToolbarComponent,
        FormsModule,
        ToolbarModule,
        TooltipModule,
        DotSafeHtmlPipe,
        DotGlobalMessageComponent,
        DotFavoritePageComponent,
        DotEditPageNavDirective,
        RouterLink,
        TagModule,
        DotMessagePipe
    ],
    exports: [DotEditPageToolbarComponent],
    declarations: [DotEditPageToolbarComponent],
    providers: [DialogService, DotPropertiesService]
})
export class DotEditPageToolbarModule {}
