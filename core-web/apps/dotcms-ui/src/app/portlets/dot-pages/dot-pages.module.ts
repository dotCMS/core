import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotESContentService, DotPageRenderService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';

import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesRoutingModule } from './dot-pages-routing.module';
import { DotPagesComponent } from './dot-pages.component';

@NgModule({
    declarations: [DotPagesComponent],
    imports: [
        CommonModule,
        DotPagesRoutingModule,
        DotPagesCardModule,
        DotPagesCardEmptyModule,
        DotMessagePipeModule,
        DotIconModule,
        PanelModule,
        ButtonModule,
        TabViewModule
    ],
    providers: [
        DotESContentService,
        DotRouterService,
        DialogService,
        DotPageRenderService,
        DotTempFileUploadService
    ]
})
export class DotPagesModule {}
