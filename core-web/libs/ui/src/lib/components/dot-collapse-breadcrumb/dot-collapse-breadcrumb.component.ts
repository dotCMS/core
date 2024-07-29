import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChevronRightIcon } from 'primeng/icons/chevronright';
import { MenuModule } from 'primeng/menu';

@Component({
    imports: [ChevronRightIcon, ButtonModule, MenuModule, RouterLink, RouterLinkActive],
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
    $model = input<MenuItem[]>([], { alias: 'model' });
    $maxItems = input<number>(4, { alias: 'maxItems' });

    onItemClick = output<{ originalEvent: Event; item: MenuItem }>();

    $itemsToShow = computed(() => {
        const items = this.$model();
        const size = items.length;
        const maxItems = this.$maxItems();

        return size > maxItems ? items.slice(size - maxItems) : items;
    });
    $itemsToHide = computed(() => {
        const items = this.$model();
        const size = items.length;
        const maxItems = this.$maxItems();

        return size > maxItems ? items.slice(0, size - maxItems) : [];
    });
    $isCollapsed = computed(() => this.$itemsToHide().length > 0);

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
