import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

import { DotESContentService, DotFavoritePageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-bookmark',
    imports: [ButtonModule, DotMessagePipe, TooltipModule],
    templateUrl: './dot-uve-bookmark.component.html',
    styleUrl: './dot-uve-bookmark.component.scss',
    providers: [DotFavoritePageService, DotESContentService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEBookmarkComponent {
    private readonly dotFavoritePageService = inject(DotFavoritePageService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly store = inject(UVEStore);

    protected $favoritePage = signal<DotCMSContentlet | null>(null);
    protected $bookmarked = signal(false);
    protected $loading = signal(false);

    constructor() {
        effect(() => {
            const pageAssetData = this.store.pageAssetData();

            if (pageAssetData) {
                untracked(() => this.fetchFavoritePage(this.store.url()));
            }
        });
    }

    toggleBookmark(): void {
        // this.dialogService.open(DotFavoritePageComponent, {
        //     header: this.dotMessageService.get('favoritePage.dialog.header'),
        //     width: '80rem',
        //     data: {
        //         page: {
        //             favoritePageUrl: this.url,
        //             favoritePage: this.favoritePage
        //         },
        //         onSave: (favoritePageUrl: string) => {
        //             this.fetchFavoritePage(favoritePageUrl);
        //         },
        //         onDelete: (favoritePageUrl: string) => {
        //             this.fetchFavoritePage(favoritePageUrl);
        //         }
        //     }
        // });
    }

    /**
     * Fetch favorite page
     *
     * @private
     * @param {string} url
     * @memberof DotEmaBookmarksComponent
     */
    private fetchFavoritePage(url: string): void {
        this.$loading.set(true);
        const userId = this.store.currentUser()?.userId ?? '';
        this.dotFavoritePageService
            .get({ url, userId, limit: 10 })
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                map((res) => res.jsonObjectView.contentlets[0])
            )
            .subscribe((favoritePage) => {
                this.$loading.set(false);
                this.$bookmarked.set(!!favoritePage);
                this.$favoritePage.set(favoritePage);
            });
    }
}
