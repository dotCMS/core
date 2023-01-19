import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';

import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import {
    DotESContentService,
    DotLanguagesService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
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
        DotPipesModule,
        DotIconModule,
        InputTextModule,
        PanelModule,
        ButtonModule,
        SkeletonModule,
        TableModule,
        TabViewModule
    ],
    providers: [
        DotESContentService,
        DotRouterService,
        DotLanguagesService,
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService
    ]
})
export class DotPagesModule {}
