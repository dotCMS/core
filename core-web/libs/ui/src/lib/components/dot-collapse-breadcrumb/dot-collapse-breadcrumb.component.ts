import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, viewChild } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChevronRightIcon } from 'primeng/icons/chevronright';
import { MenuModule, Menu } from 'primeng/menu';

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
    $maxItems = input<number>(5, { alias: 'maxItems' });
    $menuElement = viewChild<Menu>('menu');

    toogleMenu(event: Event): void {
        this.$menuElement().toggle({
            ...event,
            relativeAlign: true
        });
    }
}
