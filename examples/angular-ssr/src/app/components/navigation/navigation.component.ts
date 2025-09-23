import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotCMSNavigationItem } from '@dotcms/types';

@Component({
    selector: 'app-navigation',
    imports: [RouterLink],
    template: `
        <nav>
            <ul class="flex space-x-4 text-white">
                <li>
                    <a [routerLink]="'/'">Home</a>
                </li>
                @for (item of items(); track $index) {
                    <li>
                        <a [routerLink]="item.href" target="{{ item.target }}">{{ item.title }}</a>
                    </li>
                }
            </ul>
        </nav>
    `,
    styleUrl: './navigation.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavigationComponent {
    items = input.required<DotCMSNavigationItem[]>();
}
