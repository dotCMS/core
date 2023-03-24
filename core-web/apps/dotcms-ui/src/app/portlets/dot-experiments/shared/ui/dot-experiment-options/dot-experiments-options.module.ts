import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotExperimentOptionContentDirective } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/directives/dot-experiment-option-content.directive';
import { DotExperimentOptionsItemDirective } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/directives/dot-experiment-options-item.directive';
import { DotExperimentOptionsComponent } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/dot-experiment-options.component';

@NgModule({
    declarations: [
        DotExperimentOptionsComponent,
        DotExperimentOptionContentDirective,
        DotExperimentOptionsItemDirective
    ],
    imports: [CommonModule, ButtonModule],
    providers: [],
    exports: [
        DotExperimentOptionsComponent,
        DotExperimentOptionContentDirective,
        DotExperimentOptionsItemDirective
    ]
})
export class DotExperimentsOptionsModule {}
