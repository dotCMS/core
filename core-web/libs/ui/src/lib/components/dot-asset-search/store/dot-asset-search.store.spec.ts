import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ESOrderDirection } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotAssetSearchStore } from './dot-asset-search.store';

import { DotLanguageService } from '../../../services/dot-language/dot-language.service';
import { DotContentSearchService } from '../../../services/search/dot-content-search.service';

const EMPTY_CONTENTLET: DotCMSContentlet = {
    inode: '14dd5ad9-55ae-42a8-a5a7-e259b6d0901a',
    variantId: 'DEFAULT',
    locked: false,
    stInode: 'd5ea385d-32ee-4f35-8172-d37f58d9cd7a',
    contentType: 'Image',
    height: 4000,
    identifier: '93ca45e0-06d2-4eef-be1d-79bd6bf0fc99',
    hasTitleImage: true,
    sortOrder: 0,
    hostName: 'demo.dotcms.com',
    extension: 'jpg',
    isContent: true,
    baseType: 'FILEASSETS',
    archived: false,
    working: true,
    live: true,
    isContentlet: true,
    languageId: 1,
    titleImage: 'fileAsset',
    hasLiveVersion: true,
    deleted: false,
    folder: '',
    host: '',
    modDate: '',
    modUser: '',
    modUserName: '',
    owner: '',
    title: '',
    url: '',
    contentTypeIcon: 'assessment',
    __icon__: 'Icon'
};

const EMPTY_IMAGE_CONTENTLET = {
    mimeType: 'image/jpeg',
    type: 'file_asset',
    fileAssetVersion: 'https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg',
    fileAsset: 'https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885__480.jpg',
    ...EMPTY_CONTENTLET
};

export const IMAGE_CONTENTLETS_MOCK = [
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '1 rain-forest-view.jpg',
        name: 'rain-forest-view.jpg',
        description: 'rain-forest-view',
        title: 'Rain-forest-view.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset:
            'https://previews.123rf.com/images/rglinsky/rglinsky1201/rglinsky120100188/12336990-vertical-de-la-imagen-orientada-a-la-famosa-torre-eiffel-en-par%C3%ADs-francia-.jpg',
        fileName: '2 Foto8.jpg',
        name: 'Foto8.jpg',
        description: 'Foto8',
        title: 'Foto8.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset:
            'https://www.freesvgdownload.com/wp-content/uploads/2021/12/It-Takes-a-Big-Heart-To-Help.jpg',
        fileName: '3 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileAsset: 'https://interactive-examples.mdn.mozilla.net/media/examples/plumeria.jpg',
        fileName: '4 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '5 services-2.jpg',
        name: 'services-2.jpg',
        description: 'Backcountry Skiing Services',
        title: 'services-2.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '6 template-breadcrumbs.png',
        name: 'template-breadcrumbs.png',
        description: 'Thumbnail image for template with breadcrumbs',
        title: 'template-breadcrumbs.png'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '7 downloading.jpg',
        name: 'downloading.jpg',
        description:
            'With the opening our our new Peak Bar  this year our Top Expressive lift has improve access for downloading',
        title: 'Going Up / Going Down'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '8 resort-cottage.jpg',
        name: 'resort-cottage.jpg',
        description: 'resort-cottage',
        title: 'Resort-cottage.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '9 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '10 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '11 downloading.jpg',
        name: 'downloading.jpg',
        description:
            'With the opening our our new Peak Bar  this year our Top Expressive lift has improve access for downloading',
        title: 'Going Up / Going Down'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '12 rain-forest-view.jpg',
        name: 'rain-forest-view.jpg',
        description: 'rain-forest-view',
        title: 'Rain-forest-view.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '13 rain-forest-view.jpg',
        name: 'rain-forest-view.jpg',
        description: 'rain-forest-view',
        title: 'Rain-forest-view.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '14 Foto8.jpg',
        name: 'Foto8.jpg',
        description: 'Foto8',
        title: 'Foto8.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '15 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '16 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '17 services-2.jpg',
        name: 'services-2.jpg',
        description: 'Backcountry Skiing Services',
        title: 'services-2.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '18 template-breadcrumbs.png',
        name: 'template-breadcrumbs.png',
        description: 'Thumbnail image for template with breadcrumbs',
        title: 'template-breadcrumbs.png'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '19 downloading.jpg',
        name: 'downloading.jpg',
        description:
            'With the opening our our new Peak Bar  this year our Top Expressive lift has improve access for downloading',
        title: 'Going Up / Going Down'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '20 resort-cottage.jpg',
        name: 'resort-cottage.jpg',
        description: 'resort-cottage',
        title: 'Resort-cottage.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '21 first-chair.jpg',
        name: 'first-chair.jpg',
        description: 'Stay at one of our resorts and get early hours with our first chair program.',
        title: 'First to the Top'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '22 adult-antioxidant.jpg',
        name: 'adult-antioxidant.jpg',
        description: 'adult-antioxidant',
        title: 'Adult-antioxidant.jpg'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '23 downloading.jpg',
        name: 'downloading.jpg',
        description:
            'With the opening our our new Peak Bar  this year our Top Expressive lift has improve access for downloading',
        title: 'Going Up / Going Down'
    },
    {
        ...EMPTY_IMAGE_CONTENTLET,
        fileName: '24 rain-forest-view.jpg',
        name: 'rain-forest-view.jpg',
        description: 'rain-forest-view',
        title: 'Rain-forest-view.jpg'
    }
];

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
    let DotContentSearchService: DotContentSearchService;

    beforeEach(() => {
        TestBed.configureTestingModule({
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
                    provide: DotLanguageService,
                    useValue: {
                        getLanguages: () => of(LanguageMock)
                    }
                }
            ]
        });

        service = TestBed.inject(DotAssetSearchStore);
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

    describe('Effects', () => {
        beforeEach(() => {
            DotContentSearchService = TestBed.inject(DotContentSearchService);
        });

        it('should search contentlets based on a search query', (done) => {
            const contentlets = CONTENTLETS_MOCK_WITH_LANG.splice(0, 2);
            // Spies
            // const loadingMock = spyOn(service, 'updateLoading');
            spyOn(service, 'updateSearch');
            spyOn(service, 'updateContentlets');
            spyOn(DotContentSearchService, 'get').and.returnValue(
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

            expect(DotContentSearchService.get).toHaveBeenCalledWith({
                query: `+catchall:${query}* +title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirection.ASC,
                limit: 20,
                offset: 0
            });

            // First -> the value is true because we start the search
            // Second -> the value is false because we finish the search
            // expect(loadingMock.calls).toEqual([[true], [false]]);
            expect(service.updateContentlets).toHaveBeenCalledWith(contentlets);
            expect(service.updateSearch).toHaveBeenCalledWith(query);
        });

        it('should not add "*" when the search has a "-" ', (done) => {
            const contentlets = CONTENTLETS_MOCK_WITH_LANG;
            spyOn(DotContentSearchService, 'get').and.returnValue(
                of({ jsonObjectView: { contentlets } })
            );

            const query = 'hola-';

            service.searchContentlet(query);

            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    preventScroll: false,
                    loading: false,
                    contentlets
                });

                done();
            });

            expect(DotContentSearchService.get).toHaveBeenCalledWith({
                query: `+catchall:${query} +title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirection.ASC,
                limit: 20,
                offset: 0
            });
        });

        it('should load the next batch of contentlets based on the offset', (done) => {
            const contentlets_p1 = [...CONTENTLETS_MOCK_WITH_LANG].splice(0, 2);
            const contentlets_p2 = [...CONTENTLETS_MOCK_WITH_LANG].splice(2, 2);

            // Spies
            spyOn(service, 'updateSearch');
            spyOn(service, 'updateContentlets');
            spyOn(DotContentSearchService, 'get').and.returnValue(
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

            expect(DotContentSearchService.get).toHaveBeenCalledWith({
                query: ` +catchall:${query}* title:'${query}'^15 +languageId:1 +baseType:(4 OR 9) +metadata.contenttype:image/* +deleted:false +working:true`,
                sortOrder: ESOrderDirection.ASC,
                limit: 20,
                offset
            });
            expect(service.updateContentlets).toHaveBeenCalledWith(contentlets_p1);
        });

        it('should set preventScroll to true when backend date is coming empty', (done) => {
            const contentlets = [...CONTENTLETS_MOCK_WITH_LANG].splice(0, 2);

            // Spies
            spyOn(service, 'updateSearch');
            spyOn(service, 'updateContentlets');
            spyOn(DotContentSearchService, 'get').and.returnValue(
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
