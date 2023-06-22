import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotExperimentOptionContentDirective } from './directives/dot-experiment-option-content.directive';
import { DotExperimentOptionsItemDirective } from './directives/dot-experiment-options-item.directive';
import { DotExperimentOptionsComponent } from './dot-experiment-options.component';

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
