import { describe, expect, it } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator';
import { of } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import { DotFavoritePageService, DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaBookmarksComponent } from './dot-ema-bookmarks.component';

import { mockCurrentUser } from '../../../../../shared/mocks';
import { UVEStore } from '../../../../../store/dot-uve.store';

const mockStore = {
    $previewMode: signal(false),
    currentUser: signal(mockCurrentUser)
};

describe('DotEmaBookmarksComponent', () => {
    let spectator: Spectator<DotEmaBookmarksComponent>;

    const createComponent = createComponentFactory({
        component: DotEmaBookmarksComponent,
        imports: [ButtonModule, DotMessagePipe, AsyncPipe, HttpClientTestingModule],
        providers: [
            DialogService,
            HttpClient,
            mockProvider(UVEStore, mockStore),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },

            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: DotFavoritePageService,
                useValue: {
                    get({ url }: { url: string }) {
                        return of(
                            {
                                '/true': {
                                    jsonObjectView: {
                                        contentlets: [
                                            {
                                                identifier: '123'
                                            }
                                        ]
                                    }
                                },
                                '/false': {
                                    jsonObjectView: {
                                        contentlets: []
                                    }
                                }
                            }[url]
                        );
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                url: '/true'
            }
        });
    });

    it('should set bookmarked as true when favorite page is found', () => {
        const spectatorWithTrueBookmark = createComponent({
            props: {
                url: '/true'
            }
        });

        const button = spectatorWithTrueBookmark.debugElement.query(
            By.css('[data-testId="bookmark-button"]')
        ).componentInstance;

        expect(button.icon).toBe('pi pi-star-fill');
    });

    it("should set bookmarked as false when favorite page isn't found", async () => {
        const spectatorWithFalseBookmark = createComponent({
            props: {
                url: '/false'
            }
        });

        const button = spectatorWithFalseBookmark.debugElement.query(
            By.css('[data-testId="bookmark-button"]')
        ).componentInstance;

        expect(button.icon).toBe('pi pi-star');
    });

    it('should open a dynamic dialog when toggleBookmark is called', () => {
        const dialogService = spectator.inject(DialogService);
        const dialogServiceOpenSpy = jest.spyOn(dialogService, 'open');

        const button = spectator.debugElement.query(By.css('[data-testId="bookmark-button"]'));

        button.triggerEventHandler('click');

        expect(dialogServiceOpenSpy).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                header: expect.anything(),
                width: expect.anything(),
                data: expect.anything()
            })
        );
    });
});
