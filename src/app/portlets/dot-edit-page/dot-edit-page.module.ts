import { DotEditLayoutGuardService } from './shared/services/dot-edit-layout-guard/dot-edit-layout-guard.service';
import { DotContentletLockerService } from '../../api/services/dot-contentlet-locker/dot-contentlet-locker.service';
import { PageViewService } from '../../api/services/page-view/page-view.service';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import {
    DotTemplateAdditionalActionsModule
} from './layout/components/dot-template-additional-actions/dot-template-additional-actions.module';
import { TemplateContainersCacheService } from './template-containers-cache.service';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';
import { DotEditContentResolver } from './content/services/dot-edit-content-resolver.service';
import { DotRenderHTMLService } from '../../api/services/dot-render-html/dot-render-html.service';
import { DotDirectivesModule } from '../../shared/dot-directives.module';
import { DotPageStateService } from './content/services/dot-page-state/dot-page-state.service';

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
        DotEditContentResolver,
        DotEditLayoutGuardService,
        DotPageStateService,
        DotRenderHTMLService,
        PageViewService,
        TemplateContainersCacheService
    ]
})
export class DotEditPageModule {}
