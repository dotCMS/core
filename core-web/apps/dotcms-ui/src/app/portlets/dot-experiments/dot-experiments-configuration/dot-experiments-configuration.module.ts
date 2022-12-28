import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotExperimentsConfigurationRoutingModule } from './dot-experiments-configuration-routing.module';
import { DotExperimentsConfigurationComponent } from './dot-experiments-configuration.component';
import { DotExperimentsConfigurationExperimentStatusBarComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-experiment-status-bar/dot-experiments-configuration-experiment-status-bar.component';
import { DotExperimentsConfigurationVariantsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants/dot-experiments-configuration-variants.component';
import { DotExperimentsConfigurationTargetingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-targeting/dot-experiments-configuration-targeting.component';
import { DotExperimentsConfigurationTrafficComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic/dot-experiments-configuration-traffic.component';
import { DotExperimentsConfigurationSchedulingComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling/dot-experiments-configuration-scheduling.component';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-skeleton/dot-experiments-configuration-skeleton.component';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { ButtonModule } from 'primeng/button';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotSessionStorageService } from '@dotcms/data-access';

@NgModule({
    declarations: [DotExperimentsConfigurationComponent],
    imports: [
        CommonModule,
        // dotCMS
        DotExperimentsConfigurationRoutingModule,
        DotExperimentsConfigurationExperimentStatusBarComponent,
        DotExperimentsConfigurationVariantsComponent,
        DotExperimentsConfigurationGoalsComponent,
        DotExperimentsConfigurationTargetingComponent,
        DotExperimentsConfigurationTrafficComponent,
        DotExperimentsConfigurationSchedulingComponent,
        DotExperimentsConfigurationSkeletonComponent,
        DotExperimentsUiHeaderComponent,
        DotDynamicDirective,
        // PrimeNg
        ButtonModule
    ],
    providers: [DotSessionStorageService]
})
export class DotExperimentsConfigurationModule {}
