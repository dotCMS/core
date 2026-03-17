import { Component, inject, computed, ChangeDetectionStrategy } from '@angular/core';

import { GlobalStore } from '@dotcms/store';
import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-crumbtrail',
    templateUrl: './dot-crumbtrail.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [DotCollapseBreadcrumbComponent]
})
export class DotCrumbtrailComponent {
    /** Global store instance for accessing breadcrumb state */
    readonly #globalStore = inject(GlobalStore);

    /** Signal containing the complete breadcrumb menu items from the global store */
    $breadcrumbsMenu = this.#globalStore.breadcrumbs;

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
     * Label of the last breadcrumb, provided by the GlobalStore.
     */
    $lastBreadcrumb = this.#globalStore.selectLastBreadcrumbLabel;
}
