import { describe, expect, it } from '@jest/globals';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotFavoritePageService } from './dot-favorite-page.service';

import { DotESContentService, ESOrderDirection } from '../dot-es-content/dot-es-content.service';

describe('DotFavoritePageService', () => {
    let dotESContentService: DotESContentService;
    let dotFavoritePageService: DotFavoritePageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotESContentService,
                DotFavoritePageService
            ]
        });
        dotESContentService = TestBed.inject(DotESContentService);
        dotFavoritePageService = TestBed.inject(DotFavoritePageService);
        jest.spyOn(dotESContentService, 'get');
    });

    it('should get Favorite Pages based on an URL', () => {
        dotFavoritePageService
            .get({
                limit: 10,
                userId: '123',
                url: 'index.html'
            })
            .subscribe();

        expect(dotESContentService.get).toHaveBeenCalledWith({
            itemsPerPage: 10,
            offset: '0',
            query: '+contentType:dotFavoritePage +deleted:false +working:true +owner:123 +DotFavoritePage.url_dotraw:index.html',
            sortField: 'dotFavoritePage.order',
            sortOrder: ESOrderDirection.ASC
        });
    });

    it('should get Favorite Pages based on an Identifier', () => {
        dotFavoritePageService
            .get({
                limit: 10,
                userId: '123',
                identifier: '1'
            })
            .subscribe();

        expect(dotESContentService.get).toHaveBeenCalledWith({
            itemsPerPage: 10,
            offset: '0',
            query: '+contentType:dotFavoritePage +deleted:false +working:true +owner:123 +identifier:1',
            sortField: 'dotFavoritePage.order',
            sortOrder: ESOrderDirection.ASC
        });
    });
});
