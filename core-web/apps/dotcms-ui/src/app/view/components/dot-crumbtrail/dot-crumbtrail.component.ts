import { Component, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DotCrumbtrailService } from './service/dot-crumbtrail.service';
@Component({
    selector: 'dot-crumbtrail',
    templateUrl: './dot-crumbtrail.component.html',
    styleUrls: ['./dot-crumbtrail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotCrumbtrailComponent {
    /** Service responsible for managing breadcrumb data */
    readonly #crumbTrailService = inject(DotCrumbtrailService);

    /** Signal containing the complete breadcrumb menu items */
    $breadcrumbsMenu = toSignal(this.#crumbTrailService.crumbTrail$, {
        initialValue: []
    });

    /**
     * Computed signal containing collapsed breadcrumb items.
     *
     * Returns all breadcrumb items except the last one, which represents
     * the current page. If there's only one breadcrumb, returns an empty array.
     *
     * @returns Array of breadcrumb items to be displayed in collapsed state
     */
    $collapsedBreadcrumbs = computed(() => {
        const crumbs = this.$breadcrumbsMenu();

        return crumbs.length > 1 ? crumbs.slice(0, -1) : [];
    });

    /**
     * Computed signal containing the last breadcrumb item.
     *
     * Returns the label of the last breadcrumb item, which represents
     * the current page. If no breadcrumbs exist, returns null.
     *
     * @returns The label of the current page breadcrumb, or null if no breadcrumbs exist
     */
    $lastBreadcrumb = computed(() => {
        const crumbs = this.$breadcrumbsMenu();
        const last = crumbs.length ? crumbs.at(-1) : null;

        return last?.label ?? null;
    });
}
