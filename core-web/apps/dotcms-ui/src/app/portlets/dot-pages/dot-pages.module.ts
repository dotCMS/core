import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPagesComponent } from './dot-pages.component';
import { DotPagesRoutingModule } from './dot-pages-routing.module';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotESContentService, DotMessageService, DotPageRenderService } from '@dotcms/data-access';

import { TabViewModule } from 'primeng/tabview';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';

@NgModule({
    declarations: [DotPagesComponent],
    imports: [
        CommonModule,
        DotPagesRoutingModule,
        DotPagesCardModule,
        DotPagesCardEmptyModule,
        DotPipesModule,
        DotIconModule,
        PanelModule,
        ButtonModule,
        TabViewModule
    ],
    providers: [
        DotESContentService,
        DotRouterService,
        DialogService,
        DotMessageService,
        DotPageRenderService,
        DotTempFileUploadService
    ]
})
export class DotPagesModule {}
