import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotPageStateService } from './content/services/dot-page-state/dot-page-state.service';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFeatureFlagResolver } from '@portlets/shared/resolvers/dot-feature-flag-resolver.service';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutModule,
        DotEditPageMainModule,
        DotEditPageRoutingModule,
        DotDirectivesModule,
        DotPipesModule
    ],
    declarations: [],
    providers: [
        DotContentletLockerService,
        DotEditPageResolver,
        DotESContentService,
        DotPageStateService,
        DotPageRenderService,
        DotPageLayoutService,
        DotFeatureFlagResolver
    ]
})
export class DotEditPageModule {}
