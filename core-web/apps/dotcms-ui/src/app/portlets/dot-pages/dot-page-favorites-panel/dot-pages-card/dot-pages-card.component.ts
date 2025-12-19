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
    /** The action button id. */
    readonly $actionButtonId = input<string>('', { alias: 'actionButtonId' });
    /** The image uri. */
    readonly $imageUri = input<string>('', { alias: 'imageUri' });
    /** The title. */
    readonly $title = input<string>('', { alias: 'title' });
    /** The url. */
    readonly $url = input<string>('', { alias: 'url' });

    /** Emits when the edit button is clicked. */
    readonly edit = output<boolean>();
    /** Emits when the navigate to page button is clicked. */
    readonly navigateToPage = output<boolean>();
    /** Emits when the actions menu is opened. */
    readonly openMenu = output<MouseEvent>();
}
