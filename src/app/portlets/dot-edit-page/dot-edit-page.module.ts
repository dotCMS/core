import { PageViewResolver } from './dot-edit-page-resolver.service';
import { PageViewService } from './../../api/services/page-view/page-view.service';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout/dot-edit-layout.module';
import { DotTemplateAdditionalActionsModule } from './layout/dot-template-additional-actions/dot-template-additional-actions.module';

@NgModule({
    imports: [
        CommonModule,
        DotEditPageRoutingModule,
        DotEditLayoutModule,
        DotTemplateAdditionalActionsModule
    ],
    declarations: [],
    providers: [PageViewService, PageViewResolver]
})
export class DotEditPageModule {}
