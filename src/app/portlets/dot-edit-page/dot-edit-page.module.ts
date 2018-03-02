import { EditLayoutResolver } from './layout/services/dot-edit-layout-resolver/dot-edit-layout-resolver.service';
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
import { EditContentResolver } from './content/services/dot-edit-content-resolver.service';
import { EditPageService } from '../../api/services/edit-page/edit-page.service';
import { DotDirectivesModule } from '../../shared/dot-directives.module';

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
    providers: [EditContentResolver, EditLayoutResolver, EditPageService, PageViewService, TemplateContainersCacheService]
})
export class DotEditPageModule {}
