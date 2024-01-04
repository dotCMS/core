import { BehaviorSubject } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { map, switchMap } from 'rxjs/operators';

import { DotFavoritePageService, DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFavoritePageComponent } from '../edit-ema-editor/components/dot-favorite-page/dot-favorite-page.component';

@Component({
    selector: 'dot-ema-bookmarks',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe, AsyncPipe],
    templateUrl: './dot-ema-bookmarks.component.html',
    styleUrls: ['./dot-ema-bookmarks.component.scss']
})
export class DotEmaBookmarksComponent implements OnInit {
    @Input() url = '';
    private readonly loginService = inject(LoginService);
    private readonly dotFavoritePageService = inject(DotFavoritePageService);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);
    bookmarked = false;
    // I used a boolean at first, but for some reason the dom wasn't updating when I changed the value
    loading = new BehaviorSubject(true);
    favoritePage: DotCMSContentlet;

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

    private fetchFavoritePage(url: string): void {
        this.loading.next(true);

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
                this.loading.next(false);
                this.bookmarked = !!favoritePage;
                this.favoritePage = favoritePage;
            });
    }
}
