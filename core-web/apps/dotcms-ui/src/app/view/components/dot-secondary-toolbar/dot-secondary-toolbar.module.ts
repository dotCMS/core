import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotSecondaryToolbarComponent } from '@components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotExperimentClassDirective } from '@portlets/shared/directives/dot-experiment-class.directive';

@NgModule({
    imports: [CommonModule, DotExperimentClassDirective],
    declarations: [DotSecondaryToolbarComponent],
    exports: [DotSecondaryToolbarComponent],
    providers: []
})
export class DotSecondaryToolbarModule {}
