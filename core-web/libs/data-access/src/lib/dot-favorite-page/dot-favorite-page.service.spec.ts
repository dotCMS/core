import { describe, expect, it } from '@jest/globals';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';

import { DotFavoritePageService } from './dot-favorite-page.service';

import { DotESContentService, ESOrderDirection } from '../dot-es-content/dot-es-content.service';

describe('DotFavoritePageService', () => {
    let injector: TestBed;
    let dotESContentService: DotESContentService;
    let dotFavoritePageService: DotFavoritePageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotESContentService,
                DotFavoritePageService
            ]
        });
        injector = getTestBed();
        dotESContentService = injector.inject(DotESContentService);
        dotFavoritePageService = injector.inject(DotFavoritePageService);
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
