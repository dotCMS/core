import { describe, expect, it } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { AsyncPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotESContentService,
    DotFavoritePageService,
    DotMessageService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEmaBookmarksComponent } from './dot-ema-bookmarks.component';

describe('DotEmaBookmarksComponent', () => {
    let spectator: Spectator<DotEmaBookmarksComponent>;

    const createComponent = createComponentFactory({
        component: DotEmaBookmarksComponent,
        imports: [ButtonModule, DotMessagePipe, AsyncPipe, HttpClientTestingModule],
        providers: [
            DialogService,
            DotFavoritePageService,
            DotESContentService,
            HttpClient,
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },

            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                url: ''
            }
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
