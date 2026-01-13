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
    DotSessionStorageService,
    DotAppsService
} from '@dotcms/data-access';
import {
    DotExperimentExperimentResolver,
    DotExperimentsConfigResolver
} from '@dotcms/portlets/dot-experiments/data-access';
import { DotEnterpriseLicenseResolver, DotPushPublishEnvironmentsResolver } from '@dotcms/ui';

import { DotEditContentComponent } from './content/dot-edit-content.component';
import { dotEditPageRoutes } from './dot-edit-page.routes';
import { DotEditLayoutComponent } from './layout/dot-edit-layout/dot-edit-layout.component';
import { DotEditPageMainComponent } from './main/dot-edit-page-main/dot-edit-page-main.component';

import { DotDirectivesModule } from '../../shared/dot-directives.module';
import { DotFeatureFlagResolver } from '../shared/resolvers/dot-feature-flag-resolver.service';

@NgModule({
    imports: [
        CommonModule,
        DotEditLayoutComponent,
        DotEditPageMainComponent,
        DotEditContentComponent,
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
