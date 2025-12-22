import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { MAX_ITEMS } from './dot-collapse-breadcrumb.costants';
/**
 * Component to display a breadcrumb with a collapse button
 *
 * @export
 * @class DotCollapseBreadcrumbComponent
 */
@Component({
    imports: [ButtonModule, MenuModule, RouterModule, BreadcrumbModule],
    selector: 'dot-collapse-breadcrumb',
    templateUrl: './dot-collapse-breadcrumb.component.html',
    styleUrls: ['./dot-collapse-breadcrumb.component.scss'],
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
     * Style class to apply to the component
     *
     * @memberof DotCollapseBreadcrumbComponent
     */
    $styleClass = input<string>('', { alias: 'styleClass' });
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
