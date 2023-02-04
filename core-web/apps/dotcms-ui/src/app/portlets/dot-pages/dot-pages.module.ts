import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PanelModule } from 'primeng/panel';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import {
    DotESContentService,
    DotLanguagesService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotPageRenderService
} from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesRoutingModule } from './dot-pages-routing.module';
import { DotPagesComponent } from './dot-pages.component';

@NgModule({
    declarations: [DotPagesComponent],
    imports: [
        CommonModule,
        CheckboxModule,
        DropdownModule,
        DotActionMenuButtonModule,
        DotPagesRoutingModule,
        DotPagesCardModule,
        DotPagesCardEmptyModule,
        DotMessagePipeModule,
        DotIconModule,
        InputTextModule,
        MenuModule,
        PanelModule,
        ButtonModule,
        SkeletonModule,
        TableModule,
        TabViewModule,
        TooltipModule,
        UiDotIconButtonModule,
        DotAddToBundleModule,
        DotAutofocusModule
    ],
    providers: [
        DotESContentService,
        DotRouterService,
        DotLanguagesService,
        DotWorkflowsActionsService,
        DotWorkflowEventHandlerService,
        DotWorkflowActionsFireService,
        DialogService,
        DotPageRenderService,
        DotTempFileUploadService
    ]
})
export class DotPagesModule {}
