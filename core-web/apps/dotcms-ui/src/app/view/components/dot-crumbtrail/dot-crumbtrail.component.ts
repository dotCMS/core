import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { BreadcrumbModule } from 'primeng/breadcrumb';

import { DotCrumbtrailService } from './service/dot-crumbtrail.service';

@Component({
    selector: 'dot-crumbtrail',
    template: '<p-breadcrumb [model]="$model()" />',
    styleUrls: ['./dot-crumbtrail.component.scss'],
    standalone: true,
    imports: [BreadcrumbModule],
    providers: [DotCrumbtrailService]
})
export class DotCrumbtrailComponent {
    readonly #crumbTrailService = inject(DotCrumbtrailService);
    $model = toSignal(this.#crumbTrailService.crumbTrail$, {
        initialValue: []
    });
}
