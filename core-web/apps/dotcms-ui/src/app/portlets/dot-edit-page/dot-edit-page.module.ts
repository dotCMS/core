import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import {
    DotContentletLockerService,
    DotESContentService,
    DotEditPageResolver,
    DotExperimentsService,
    DotFavoritePageService,
    DotPageLayoutService,
    DotPageRenderService,
    DotPageStateService,
    DotSessionStorageService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver
} from '@dotcms/portlets/dot-experiments/data-access';
import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { dotEditPageRoutes } from './dot-edit-page.routes';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';

import { DotAppsService } from '../../api/services/dot-apps/dot-apps.service';
import { DotDirectivesModule } from '../../shared/dot-directives.module';
import { DotFeatureFlagResolver } from '../shared/resolvers/dot-feature-flag-resolver.service';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutModule,
        DotEditPageMainModule,
        RouterModule.forChild(dotEditPageRoutes),
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
