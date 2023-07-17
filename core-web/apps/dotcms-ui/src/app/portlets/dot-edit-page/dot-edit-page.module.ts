import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotFavoritePageService } from '@dotcms/app/api/services/dot-favorite-page/dot-favorite-page.service';
import {
    DotContentletLockerService,
    DotESContentService,
    DotPageLayoutService,
    DotPageRenderService,
    DotSessionStorageService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver,
    DotExperimentsService
} from '@dotcms/portlets/dot-experiments/data-access';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import {
    DotPushPublishEnvironmentsResolver,
    DotEnterpriseLicenseResolver
} from '@portlets/shared/resolvers';
import { DotFeatureFlagResolver } from '@portlets/shared/resolvers/dot-feature-flag-resolver.service';
import { DotDirectivesModule } from '@shared/dot-directives.module';

import { DotPageStateService } from './content/services/dot-page-state/dot-page-state.service';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';

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
        DotExperimentExperimentResolver,
        DotExperimentsConfigResolver,
        DotExperimentsService,
        DotESContentService,
        DotPageStateService,
        DotPageRenderService,
        DotSessionStorageService,
        DotPageLayoutService,
        DotFeatureFlagResolver,
        DotFavoritePageService,
        DotEnterpriseLicenseResolver,
        DotPushPublishEnvironmentsResolver
    ]
})
export class DotEditPageModule {}
