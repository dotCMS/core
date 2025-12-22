import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    Input,
    OnInit,
    inject,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { TooltipModule } from 'primeng/tooltip';

import { map } from 'rxjs/operators';

import { DotFavoritePageService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-ema-bookmarks',
    imports: [ButtonModule, DotMessagePipe, TooltipModule],
    templateUrl: './dot-ema-bookmarks.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaBookmarksComponent implements OnInit {
    @Input() url = '';

    private readonly dotFavoritePageService = inject(DotFavoritePageService);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);
    protected readonly store = inject(UVEStore);

    favoritePage: DotCMSContentlet;

    bookmarked = signal(false);
    loading = signal(false);

    ngOnInit(): void {
        this.fetchFavoritePage(this.url);
    }

    toggleBookmark(): void {
        this.dialogService.open(DotFavoritePageComponent, {
            header: this.dotMessageService.get('favoritePage.dialog.header'),
            width: '80rem',
            draggable: false,
            data: {
                page: {
                    favoritePageUrl: this.url,
                    favoritePage: this.favoritePage
                },
                onSave: (favoritePageUrl: string) => {
                    this.fetchFavoritePage(favoritePageUrl);
                },
                onDelete: (favoritePageUrl: string) => {
                    this.fetchFavoritePage(favoritePageUrl);
                }
            }
        });
    }

    /**
     * Fetch favorite page
     *
     * @private
     * @param {string} url
     * @memberof DotEmaBookmarksComponent
     */
    private fetchFavoritePage(url: string): void {
        this.loading.set(true);

        this.dotFavoritePageService
            .get({
                url,
                userId: this.store.currentUser()?.userId,
                limit: 10
            })
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                map((res) => res.jsonObjectView.contentlets[0])
            )
            .subscribe((favoritePage) => {
                this.loading.set(false);
                this.bookmarked.set(!!favoritePage);
                this.favoritePage = favoritePage;
            });
    }
}
