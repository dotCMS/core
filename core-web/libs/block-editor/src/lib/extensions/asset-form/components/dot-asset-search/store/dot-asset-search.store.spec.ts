import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { DotAssetSearchStore } from './dot-asset-search.store';

import { IMAGE_CONTENTLETS_MOCK } from '@dotcms/block-editor';
import { DotLanguageService } from '../../../../../shared/services/dot-language/dot-language.service';
import {
    SearchService,
    ESOrderDirection
} from '../../../../../shared/services/search/search.service';

const INITIAL_STATE = { contentlets: [], loading: true, preventScroll: false };
const LanguageMock = {
    1: {
        country: 'United States',
        countryCode: 'US',
        defaultLanguage: true,
        id: 1,
        language: 'English',
        languageCode: 'en'
    },
    2: {
        country: 'Espana',
        countryCode: 'ES',
        defaultLanguage: false,
        id: 2,
        language: 'Espanol',
        languageCode: 'es'
    }
};

const CONTENTLETS_MOCK_WITH_LANG = IMAGE_CONTENTLETS_MOCK.splice(0, 4).map((contentlet) => ({
    ...contentlet,
    language: 'en-US'
}));

describe('DotAssetSearchStore', () => {
    let service: DotAssetSearchStore;
    let searchService: SearchService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotAssetSearchStore,
                {
                    provide: SearchService,
                    useValue: {
                        get: () => of()
                    }
                },
                {
                    provide: DotLanguageService,
                    useValue: {
                        getLanguages: jest.fn().mockReturnValue(of(LanguageMock))
                    }
                }
            ]
        });

        service = TestBed.inject(DotAssetSearchStore);
    });

    test('should have inital state', (done) => {
        service.vm$.subscribe((res) => {
            expect(res).toEqual(INITIAL_STATE);
            done();
        });
    });

    describe('Updaters', () => {
        test('should update contentlets', (done) => {
            const contentlet = [IMAGE_CONTENTLETS_MOCK[0], IMAGE_CONTENTLETS_MOCK[1]];
            service.updateContentlets(contentlet);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    ...INITIAL_STATE,
                    contentlets: contentlet
                });
                done();
            });
        });

        test('should update LanguageId', (done) => {
            service.updatelanguageId(2);

            service.state$.subscribe((res) => {
                expect(res.languageId).toEqual(2);
                done();
            });
        });

        test('should update Loading', (done) => {
            service.updateLoading(false);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    ...INITIAL_STATE,
                    loading: false
                });
                done();
            });
        });

        test('should prevent Scroll', (done) => {
            service.updatePreventScroll(true);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    ...INITIAL_STATE,
                    preventScroll: true
                });
                done();
            });
        });

        test('should Search', (done) => {
            const search = 'Image';
            service.updateSearch(search);

            service.state$.subscribe((res) => {
                expect(res.search).toEqual(search);
                done();
            });
        });
    });

    describe('Effects', () => {
        beforeEach(() => {
            searchService = TestBed.inject(SearchService);
        });

        test('should search contentlets based on a search query', (done) => {
            const contentlets = CONTENTLETS_MOCK_WITH_LANG.splice(0, 2);
            // Spies
            const loadingMock = jest.spyOn(service, 'updateLoading');
            jest.spyOn(service, 'updateSearch');
            jest.spyOn(service, 'updateContentlets');
            jest.spyOn(searchService, 'get').mockReturnValue(
                of({ jsonObjectView: { contentlets } })
            );

            const query = 'image';

            service.searchContentlet(query);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets
                });

                done();
            });

            expect(searchService.get).toHaveBeenCalledWith({
                query: ` +catchall:${query}* title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirection.ASC,
                limit: 20,
                offset: 0
            });

            // First -> the value is true because we start the search
            // Second -> the value is false because we finish the search
            expect(loadingMock.mock.calls).toEqual([[true], [false]]);
            expect(service.updateContentlets).toHaveBeenCalledWith(contentlets);
            expect(service.updateSearch).toHaveBeenCalledWith(query);
        });

        test('should load the next batch of contentlets based on the offset', (done) => {
            const contentlets_p1 = [...CONTENTLETS_MOCK_WITH_LANG].splice(0, 2);
            const contentlets_p2 = [...CONTENTLETS_MOCK_WITH_LANG].splice(2, 2);

            // Spies
            jest.spyOn(service, 'updateSearch');
            jest.spyOn(service, 'updateContentlets');
            jest.spyOn(searchService, 'get').mockReturnValue(
                of({ jsonObjectView: { contentlets: contentlets_p2 } })
            );

            const query = 'image';
            const offset = 2;

            service.updateContentlets(contentlets_p1);
            service.updateSearch(query);
            service.nextBatch(offset);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets: [...contentlets_p1, ...contentlets_p2]
                });

                done();
            });

            expect(searchService.get).toHaveBeenCalledWith({
                query: ` +catchall:${query}* title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirection.ASC,
                limit: 20,
                offset
            });
            expect(service.updateContentlets).toHaveBeenCalledWith(contentlets_p1);
        });

        test('should set preventScroll to true when backend date is coming empty', (done) => {
            const contentlets = [...CONTENTLETS_MOCK_WITH_LANG].splice(0, 2);

            // Spies
            jest.spyOn(service, 'updateSearch');
            jest.spyOn(service, 'updateContentlets');
            jest.spyOn(searchService, 'get').mockReturnValue(
                of({ jsonObjectView: { contentlets: [] } })
            );

            const query = 'image';
            const offset = 2;

            service.updateContentlets(contentlets);
            service.updateSearch(query);
            service.nextBatch(offset);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: true,
                    loading: false,
                    contentlets
                });

                done();
            });
        });
    });
});
