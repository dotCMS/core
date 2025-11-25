import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { ChevronRightIcon } from 'primeng/icons/chevronright';
import { MenuModule } from 'primeng/menu';

import { MAX_ITEMS } from './dot-collapse-breadcrumb.costants';
/**
 * Component to display a breadcrumb with a collapse button
 *
 * @export
 * @class DotCollapseBreadcrumbComponent
 */
@Component({
    imports: [ChevronRightIcon, ButtonModule, MenuModule, RouterModule, BreadcrumbModule],
    standalone: true,
    selector: 'dot-collapse-breadcrumb',
    templateUrl: './dot-collapse-breadcrumb.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCollapseBreadcrumbComponent {
    /**
     * Menu items to display
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $model = input<MenuItem[]>([], { alias: 'model' });
    /**
     * Max items to display
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $maxItems = input<number>(MAX_ITEMS, { alias: 'maxItems' });
    /**
     * Items to show
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $itemsToShow = computed(() => {
        const items = this.$model();
        const size = items.length;
        const maxItems = this.$maxItems();

        return size > maxItems ? items.slice(size - maxItems) : items;
    });
    /**
     * Items to hide
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $itemsToHide = computed(() => {
        const items = this.$model();
        const size = items.length;
        const maxItems = this.$maxItems();

        return size > maxItems ? items.slice(0, size - maxItems) : [];
    });
    /**
     * Indicates if the menu is collapsed
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $isCollapsed = computed(() => this.$itemsToHide().length > 0);
    /**
     * Event emitted when a menu item is clicked
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    onItemClick = output<{ originalEvent: Event; item: MenuItem }>();
}
