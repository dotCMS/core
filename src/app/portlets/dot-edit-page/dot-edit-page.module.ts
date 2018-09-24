import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { PageViewService } from '@services/page-view/page-view.service';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import { DotTemplateAdditionalActionsModule } from './layout/components/dot-template-additional-actions/dot-template-additional-actions.module';
import { TemplateContainersCacheService } from './template-containers-cache.service';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';
import { DotRenderHTMLService } from '@services/dot-render-html/dot-render-html.service';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotPageStateService } from './content/services/dot-page-state/dot-page-state.service';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import { DotEditPageDataService } from './shared/services/dot-edit-page-resolver/dot-edit-page-data.service';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutModule,
        DotEditPageMainModule,
        DotEditPageRoutingModule,
        DotTemplateAdditionalActionsModule,
        DotDirectivesModule
    ],
    declarations: [],
    providers: [
        DotContentletLockerService,
        DotEditPageResolver,
        DotPageStateService,
        DotRenderHTMLService,
        PageViewService,
        TemplateContainersCacheService,
        DotEditPageDataService
    ]
})
export class DotEditPageModule {}
