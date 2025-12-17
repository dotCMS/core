import { Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-pages-card',
    templateUrl: './dot-pages-card.component.html',
    styleUrls: ['./dot-pages-card.component.scss'],
    imports: [CardModule, ButtonModule, TooltipModule, DotPagesFavoritePageEmptySkeletonComponent]
})
export class DotPagesCardComponent {
    readonly $actionButtonId = input<string>('', { alias: 'actionButtonId' });
    readonly $imageUri = input<string>('', { alias: 'imageUri' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $url = input<string>('', { alias: 'url' });

    readonly edit = output<boolean>();
    readonly goTo = output<boolean>();
    readonly showContextMenu = output<MouseEvent>();
}
