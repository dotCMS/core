import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';
import { DotEditLayoutModule } from './layout/dot-edit-layout.module';
import { DotEditPageMainModule } from './main/dot-edit-page-main/dot-edit-page-main.module';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotPageStateService } from './content/services/dot-page-state/dot-page-state.service';
import { DotEditPageResolver } from './shared/services/dot-edit-page-resolver/dot-edit-page-resolver.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFeatureFlagResolver } from '@portlets/shared/resolvers/dot-feature-flag-resolver.service';
import { DotExperimentExperimentResolver } from '@portlets/dot-experiments/shared/resolvers/dot-experiment-experiment.resolver';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import {
    DotContentletLockerService,
    DotESContentService,
    DotPageLayoutService,
    DotPageRenderService
} from '@dotcms/data-access';

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
        DotExperimentsService,
        DotESContentService,
        DotPageStateService,
        DotPageRenderService,
        DotPageLayoutService,
        DotFeatureFlagResolver
    ]
})
export class DotEditPageModule {}
