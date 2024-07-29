import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChevronRightIcon } from 'primeng/icons/chevronright';
import { MenuModule } from 'primeng/menu';

@Component({
    imports: [ChevronRightIcon, ButtonModule, MenuModule, JsonPipe],
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
}
