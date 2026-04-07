import { exposeComponent } from '@hashbrownai/angular';

import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AiFavoritePageCardComponent } from './favorite-page-card.component';

@Component({
    selector: 'app-favorite-page-list',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [],
    template: `
        <div class="w-full rounded-xl bg-surface-100 p-3">
            <div
                class="flex snap-x snap-mandatory gap-3 overflow-x-auto overflow-y-hidden pb-2 pt-1 [scrollbar-width:thin]">
                <ng-content />
            </div>
        </div>
    `
})
export class FavoritePageListComponent {}

export const AiFavoritePageListComponent = exposeComponent(FavoritePageListComponent, {
    description:
        'Horizontal scroll wrapper for favorite pages. Wrap one or more app-favorite-page-card elements.',
    input: {},
    children: [AiFavoritePageCardComponent]
});
