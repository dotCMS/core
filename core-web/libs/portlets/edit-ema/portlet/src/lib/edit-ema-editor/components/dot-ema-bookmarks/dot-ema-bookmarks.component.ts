import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { map, switchMap } from 'rxjs/operators';

import { DotFavoritePageService, DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFavoritePageComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-ema-bookmarks',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-ema-bookmarks.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaBookmarksComponent implements OnInit {
    @Input() url = '';

    private readonly loginService = inject(LoginService);
    private readonly dotFavoritePageService = inject(DotFavoritePageService);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

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

        this.loginService
            .getCurrentUser()
            .pipe(
                switchMap((user) => {
                    return this.dotFavoritePageService
                        .get({
                            url,
                            userId: user.userId,
                            limit: 10
                        })
                        .pipe(map((res) => res.jsonObjectView.contentlets[0]));
                })
            )
            .subscribe((favoritePage) => {
                this.loading.set(false);
                this.bookmarked.set(!!favoritePage);
                this.favoritePage = favoritePage;
            });
    }
}
