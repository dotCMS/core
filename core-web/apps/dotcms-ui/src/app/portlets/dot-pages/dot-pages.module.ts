import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';
import { MenuModule } from 'primeng/menu';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import {
    DotESContentService,
    DotFavoritePageService,
    DotLanguagesService,
    DotPageRenderService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotRouterService,
    DotSessionStorageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotPagesCreatePageDialogComponent } from './dot-pages-create-page-dialog/dot-pages-create-page-dialog.component';
import { DotPagesFavoritePanelModule } from './dot-pages-favorite-panel/dot-pages-favorite-panel.module';
import { DotPagesListingPanelModule } from './dot-pages-listing-panel/dot-pages-listing-panel.module';
import { DotPagesRoutingModule } from './dot-pages-routing.module';
import { DotPagesComponent } from './dot-pages.component';

@NgModule({
    declarations: [DotPagesComponent],
    imports: [
        CommonModule,
        DotAddToBundleComponent,
        DotPagesCreatePageDialogComponent,
        DotPagesFavoritePanelModule,
        DotPagesListingPanelModule,
        DotPagesRoutingModule,
        MenuModule,
        ProgressSpinnerModule
    ],
    providers: [
        DotSessionStorageService,
        DialogService,
        DotESContentService,
        DotLanguagesService,
        DotPageRenderService,
        DotPageTypesService,
        DotTempFileUploadService,
        DotWorkflowsActionsService,
        DotPageWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotRouterService,
        DotFavoritePageService,
        DotSessionStorageService
    ]
})
export class DotPagesModule {}
