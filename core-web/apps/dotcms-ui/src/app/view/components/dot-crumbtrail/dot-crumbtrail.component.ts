import { Component, inject, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DotCrumbtrailService } from './service/dot-crumbtrail.service';
@Component({
    selector: 'dot-crumbtrail',
    templateUrl: './dot-crumbtrail.component.html',
    styleUrls: ['./dot-crumbtrail.component.scss']
})
export class DotCrumbtrailComponent {
    readonly #crumbTrailService = inject(DotCrumbtrailService);
    $breadcrumbsMenu = toSignal(this.#crumbTrailService.crumbTrail$, {
        initialValue: []
    });

    // Collapsed items: all except last
    $collapsedBreadcrumbs = computed(() => {
        const crumbs = this.$breadcrumbsMenu();

        return crumbs.length > 1 ? crumbs.slice(0, -1) : [];
    });

    // Last item: the last breadcrumb
    $lastBreadcrumb = computed(() => {
        const crumbs = this.$breadcrumbsMenu();

        const last = crumbs.length ? crumbs.at(-1) : null;

        if (last) {
            return last.label;
        }

        return null;
    });
}
