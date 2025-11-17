import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

import { DotExperimentClassDirective } from '../../../portlets/shared/directives/dot-experiment-class.directive';

@Component({
    selector: 'dot-secondary-toolbar',
    templateUrl: './dot-secondary-toolbar.component.html',
    styleUrls: ['./dot-secondary-toolbar.component.scss'],
    imports: [CommonModule, DotExperimentClassDirective]
})
export class DotSecondaryToolbarComponent {}
