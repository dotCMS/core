import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotESContentService } from '@dotcms/data-access';
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
        DotPipesModule,
        DotIconModule,
        PanelModule,
        ButtonModule,
        TabViewModule
    ],
    providers: [DotESContentService, DotRouterService]
})
export class DotPagesModule {}
