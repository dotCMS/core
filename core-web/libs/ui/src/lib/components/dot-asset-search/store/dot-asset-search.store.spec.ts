import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentSearchService,
    DotLanguagesService,
    ESOrderDirectionSearch
} from '@dotcms/data-access';
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
    }
];

const LANGUAGE_MOCK = [
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

const CONTENTLETS_MOCK_WITH_LANG = IMAGE_CONTENTLETS_MOCK.splice(0, 4).map((contentlet) => ({
    ...contentlet,
    language: 'en-US'
}));

const INITIAL_STATE = { contentlets: [], loading: true, preventScroll: false };

describe('DotAssetSearchStore', () => {
    let spectator: SpectatorService<DotAssetSearchStore>;
    let service: DotAssetSearchStore;
    let dotContentSearchService: DotContentSearchService;

    const createService = createServiceFactory({
        service: DotAssetSearchStore,
        providers: [
            {
                provide: DotContentSearchService,
                useValue: {
                    get: () => of()
                }
            },
            {
                provide: DotLanguagesService,
                useValue: {
                    get: () => of(LANGUAGE_MOCK)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        dotContentSearchService = spectator.inject(DotContentSearchService);
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
                    preventScroll: false,
                    loading: false,
                    contentlets: contentlet
                });
                done();
            });
        });

        it('should merge contentlets', (done) => {
            const contentlet = [IMAGE_CONTENTLETS_MOCK[0], IMAGE_CONTENTLETS_MOCK[1]];
            service.mergeContentlets(contentlet);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets: [...INITIAL_STATE.contentlets, ...contentlet]
                });
                done();
            });
        });
    });

    describe('Effects', () => {
        it('should search contentlets', (done) => {
            const contentlets = CONTENTLETS_MOCK_WITH_LANG.splice(0, 2);

            const spyLoading = jest.spyOn(service, 'updateLoading');
            const spySearch = jest
                .spyOn(dotContentSearchService, 'get')
                .mockReturnValue(of({ jsonObjectView: { contentlets } }));

            const params = {
                search: 'image',
                assetType: 'image',
                languageId: 1,
                offset: 0
            };

            spectator.service.searchContentlet(params);

            spectator.service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets
                });

                done();
            });

            expect(spyLoading).toHaveBeenCalledWith(true);
            expect(spySearch).toHaveBeenCalledWith({
                query: `+catchall:${params.search}* title:'${params.search}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirectionSearch.ASC,
                limit: 20,
                offset: 0
            });
        });

        it('should load next banch', (done) => {
            const contentlets = CONTENTLETS_MOCK_WITH_LANG.splice(0, 2);
            const spySearch = jest
                .spyOn(dotContentSearchService, 'get')
                .mockReturnValue(of({ jsonObjectView: { contentlets } }));

            const params = {
                search: 'image',
                assetType: 'image',
                languageId: 1,
                offset: 10
            };

            spectator.service.nextBatch(params);

            spectator.service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets
                });

                done();
            });

            expect(spySearch).toHaveBeenCalledWith({
                query: `+catchall:${params.search}* title:'${params.search}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirectionSearch.ASC,
                limit: 20,
                offset: 10
            });
        });
    });
});
