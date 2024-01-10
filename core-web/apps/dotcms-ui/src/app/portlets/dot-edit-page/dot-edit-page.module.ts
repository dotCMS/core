import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import {
    DotContentletLockerService,
    DotESContentService,
    DotFavoritePageService,
    DotPageLayoutService,
    DotPageRenderService,
    DotSessionStorageService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver,
    DotExperimentsService
} from '@dotcms/portlets/dot-experiments/data-access';
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
        DotDirectivesModule
    ],
    declarations: [],
    providers: [
        DotAppsService,
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
