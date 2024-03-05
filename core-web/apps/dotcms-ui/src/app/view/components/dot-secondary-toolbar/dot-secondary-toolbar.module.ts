import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotSecondaryToolbarComponent } from './dot-secondary-toolbar.component';

import { DotExperimentClassDirective } from '../../../portlets/shared/directives/dot-experiment-class.directive';

@NgModule({
    imports: [CommonModule, DotExperimentClassDirective],
    declarations: [DotSecondaryToolbarComponent],
    exports: [DotSecondaryToolbarComponent],
    providers: []
})
export class DotSecondaryToolbarModule {}
