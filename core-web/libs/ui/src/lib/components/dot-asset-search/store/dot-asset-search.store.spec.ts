import { SpectatorService, createServiceFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotContentSearchService, DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { EMPTY_IMAGE_CONTENTLET } from '@dotcms/utils-testing';

import { DotAssetSearchStore } from './dot-asset-search.store';

export const IMAGE_CONTENTLETS_MOCK: DotCMSContentlet[] = [
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '1 rain-forest-view.jpg',
        name: 'rain-forest-view.jpg',
        description: 'rain-forest-view',
        title: 'Rain-forest-view.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'test2.jpg',
        fileName: '2 Foto8.jpg',
        name: 'Foto8.jpg',
        description: 'Foto8',
        title: 'Foto8.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'test3.jpg',
        fileName: '3 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'test4.jpg',
        fileName: '4 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'test5.jpg',
        fileName: '5 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'test6.jpg',
        fileName: '6 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    }
];

const INITIAL_STATE = { contentlets: [], loading: true, preventScroll: false };
const LanguageMock = [
    {
        country: 'United States',
        countryCode: 'US',
        defaultLanguage: true,
        id: 1,
        language: 'English',
        languageCode: 'en'
    },
    {
        country: 'Espana',
        countryCode: 'ES',
        defaultLanguage: false,
        id: 2,
        language: 'Espanol',
        languageCode: 'es'
    }
];

// const CONTENTLETS_MOCK_WITH_LANG = IMAGE_CONTENTLETS_MOCK.splice(0, 6).map((contentlet) => ({
//     ...contentlet,
//     language: 'en-US'
// }));

describe('DotAssetSearchStore', () => {
    let spectator: SpectatorService<DotAssetSearchStore>;
    let service: DotAssetSearchStore;
    // let dotContentSearchService: DotContentSearchService;

    const createService = createServiceFactory({
        service: DotAssetSearchStore,
        imports: [HttpClientTestingModule],
        providers: [
            DotAssetSearchStore,
            {
                provide: DotContentSearchService,
                useValue: {
                    get: () => of()
                }
            },
            {
                provide: DotLanguagesService,
                useValue: {
                    get: () => of(LanguageMock)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        // dotContentSearchService = spectator.inject(DotContentSearchService);
    });

    it('should have inital state', (done) => {
        service.vm$.subscribe((res) => {
            expect(res).toEqual(INITIAL_STATE);
            done();
        });
    });

    describe('Updaters', () => {
        it('should update contentlets', (done) => {
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

        it('should update LanguageId', (done) => {
            service.updatelanguageId(2);

            service.state$.subscribe((res) => {
                expect(res.languageId).toEqual(2);
                done();
            });
        });

        it('should update Loading', (done) => {
            service.updateLoading(false);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    ...INITIAL_STATE,
                    loading: false
                });
                done();
            });
        });

        it('should prevent Scroll', (done) => {
            service.updatePreventScroll(true);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    ...INITIAL_STATE,
                    preventScroll: true
                });
                done();
            });
        });

        it('should Search', (done) => {
            const search = 'Image';
            service.updateSearch(search);

            service.state$.subscribe((res) => {
                expect(res.search).toEqual(search);
                done();
            });
        });
    });

    // describe('Effects', () => {
    //     beforeEach(() => {
    //         DotContentSearchService = TestBed.inject(DotContentSearchService);
    //     });

    //     it('should search contentlets based on a search query', (done) => {
    //         const contentlets = CONTENTLETS_MOCK_WITH_LANG.splice(0, 2);
    //         // Spies
    //         // const loadingMock = spyOn(service, 'updateLoading');
    //         spyOn(service, 'updateSearch');
    //         spyOn(service, 'updateContentlets');
    //         spyOn(DotContentSearchService, 'get').and.returnValue(
    //             of({ jsonObjectView: { contentlets } })
    //         );

    //         const query = 'image';

    //         service.searchContentlet(query);

    //         service.vm$.subscribe((res) => {
    //             expect(res).toEqual({
    //                 preventScroll: false,
    //                 loading: false,
    //                 contentlets
    //             });

    //             done();
    //         });

    //         expect(DotContentSearchService.get).toHaveBeenCalledWith({
    //             query: `+catchall:${query}* +title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
    //             sortOrder: ESOrderDirection.ASC,
    //             limit: 20,
    //             offset: 0
    //         });

    //         // First -> the value is true because we start the search
    //         // Second -> the value is false because we finish the search
    //         // expect(loadingMock.calls).toEqual([[true], [false]]);
    //         expect(service.updateContentlets).toHaveBeenCalledWith(contentlets);
    //         expect(service.updateSearch).toHaveBeenCalledWith(query);
    //     });

    //     it('should not add "*" when the search has a "-" ', (done) => {
    //         const contentlets = CONTENTLETS_MOCK_WITH_LANG;
    //         spyOn(DotContentSearchService, 'get').and.returnValue(
    //             of({ jsonObjectView: { contentlets } })
    //         );

    //         const query = 'hola-';

    //         service.searchContentlet(query);

    //         service.vm$.subscribe((res) => {
    //             expect(res).toEqual({
    //                 preventScroll: false,
    //                 loading: false,
    //                 contentlets
    //             });

    //             done();
    //         });

    //         expect(DotContentSearchService.get).toHaveBeenCalledWith({
    //             query: `+catchall:${query} +title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
    //             sortOrder: ESOrderDirection.ASC,
    //             limit: 20,
    //             offset: 0
    //         });
    //     });

    //     it('should load the next batch of contentlets based on the offset', (done) => {
    //         const contentlets_p1 = [...CONTENTLETS_MOCK_WITH_LANG].splice(0, 2);
    //         const contentlets_p2 = [...CONTENTLETS_MOCK_WITH_LANG].splice(2, 2);

    //         // Spies
    //         spyOn(service, 'updateSearch');
    //         spyOn(service, 'updateContentlets');
    //         spyOn(DotContentSearchService, 'get').and.returnValue(
    //             of({ jsonObjectView: { contentlets: contentlets_p2 } })
    //         );

    //         const query = 'image';
    //         const offset = 2;

    //         service.updateContentlets(contentlets_p1);
    //         service.updateSearch(query);
    //         service.nextBatch(offset);

    //         service.vm$.subscribe((res) => {
    //             expect(res).toEqual({
    //                 preventScroll: false,
    //                 loading: false,
    //                 contentlets: [...contentlets_p1, ...contentlets_p2]
    //             });

    //             done();
    //         });
    //     });

    // });
});
