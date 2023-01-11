import { CommonModule } from '@angular/common';
import { DotPagesComponent } from './dot-pages.component';
import { DotPagesRoutingModule } from './dot-pages-routing.module';
import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotESContentService, DotPageRenderService } from '@dotcms/data-access';
import { TabViewModule } from 'primeng/tabview';
import { DotIconModule } from '@dotcms/ui';
import { PanelModule } from 'primeng/panel';
import { NgModule } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotTempFileUploadService } from '@dotcms/app/api/services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotPagesCardEmptyModule } from './dot-pages-card-empty/dot-pages-card-empty.module';

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
