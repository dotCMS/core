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

    /**
     * The bookmark for the current page, or `undefined` when there is none — the fetch reads
     * `contentlets[0]` of a possibly-empty list, which is exactly what `bookmarked` reflects.
     */
    favoritePage?: DotCMSContentlet;

    bookmarked = signal(false);
    loading = signal(false);

    ngOnInit(): void {
        this.fetchFavoritePage(this.url);
    }

    toggleBookmark(): void {
        // HERE
        this.dialogService.open(DotFavoritePageComponent, {
            header: this.dotMessageService.get('favoritePage.dialog.header'),
            width: '80rem',
            draggable: false,
            contentStyle: {
                display: 'flex',
                flexDirection: 'column',
                minHeight: 0,
                overflow: 'hidden'
            },
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
        const userId = this.store.uveCurrentUser()?.userId;

        // The service interpolates this straight into an Elasticsearch query as `+owner:${userId}`,
        // so an absent user would send `+owner:` — malformed rather than empty. With no current user
        // there are no favourites to fetch, and the loading flag is left off rather than started and
        // never resolved.
        if (!userId) {
            return;
        }

        this.loading.set(true);

        this.dotFavoritePageService
            .get({
                url,
                userId,
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
