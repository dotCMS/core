import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MenuItem } from 'primeng/api';
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
    imports: [ChevronRightIcon, ButtonModule, MenuModule, RouterModule],
    standalone: true,
    selector: 'dot-collapse-breadcrumb',
    templateUrl: './dot-collapse-breadcrumb.component.html',
    styleUrls: ['./dot-collapse-breadcrumb.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    // eslint-disable-next-line @angular-eslint/no-host-metadata-property
    host: {
        class: 'p-element'
    }
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
    /**
     * Collapse the menu
     *
     * @param {Event} event
     * @param {MenuItem} item
     * @memberof DotCollapseBreadcrumbComponent
     */
    itemClick(event: Event, item: MenuItem) {
        if (item.disabled) {
            event.preventDefault();

            return;
        }

        if (!item.url && !item.routerLink) {
            event.preventDefault();
        }

        if (item.command) {
            item.command({
                originalEvent: event,
                item: item
            });
        }

        this.onItemClick.emit({
            originalEvent: event,
            item: item
        });
    }
}
