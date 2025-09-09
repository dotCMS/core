import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import {
    DotContentTypeService,
    DotESContentService,
    DotPropertiesService,
    DotSessionStorageService,
    PaginatorService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType, ESContent } from '@dotcms/dotcms-models';

import { DotPaletteStore } from './dot-palette.store';

import { contentTypeDataMock } from '../dot-palette-content-type/dot-palette-content-type.component.spec';
import {
    contentletFormDataMock,
    contentletProductDataMock
} from '../dot-palette-contentlets/dot-palette-contentlets.component.spec';
import {
    ContentletWithDuplicatedMock,
    NewVariantContentletMock,
    NotDuplicatedContentletMock,
    VARIANT_ID_MOCK
} from '../mocks/contentlets.mock';

const responseData: DotCMSContentType[] = [
    {
        icon: 'cloud',
        id: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
        name: 'Widget X',
        variable: 'WidgetX',
        baseType: 'WIDGET'
    },
    {
        icon: 'alt_route',
        id: '799f176a-d32e-4844-a07c-1b5fcd107578',
        name: 'Banner',
        variable: 'Banner'
    },
    {
        icon: 'cloud',
        id: '897cf4a9-171a-4204-accb-c1b498c813fe',
        name: 'Contact',
        variable: 'Contact'
    },
    {
        icon: 'cloud',
        id: 'now-show',
        name: 'now-show',
        variable: 'persona'
    },
    {
        icon: 'cloud',
        id: 'now-show',
        name: 'now-show',
        variable: 'host'
    },
    {
        icon: 'cloud',
        id: 'now-show',
        name: 'now-show',
        variable: 'vanityurl'
    },
    {
        icon: 'cloud',
        id: 'now-show',
        name: 'now-show',
        variable: 'languagevariable'
    }
] as DotCMSContentType[];

@Injectable()
class MockPaginatorService {
    url: string;
    paginationPerPage = 10;
    maxLinksPage = 5;
    sortField: string;
    sortOrder: string;
    totalRecords = 40;

    setExtraParams(): void {
        /** */
    }

    public getWithOffset(): Observable<DotCMSContentlet[] | DotCMSContentType[]> {
        return null;
    }
}

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 15;
    totalRecords = 20;

    public get(): Observable<ESContent> {
        return null;
    }
}

@Injectable()
class MockContentTypeService {
    public getContentTypes(): Observable<ESContent> {
        return null;
    }

    public filterContentTypes(): Observable<ESContent> {
        return null;
    }
}

const SORTED_CONTENT_TYPE_MOCK = contentTypeDataMock.sort((a, b) =>
    a.name.localeCompare(b.name)
) as DotCMSContentType[];

describe('DotPaletteStore', () => {
    let dotPaletteStore: DotPaletteStore;
    let paginatorService: PaginatorService;
    let dotContentTypeService: DotContentTypeService;
    let dotESContentService: DotESContentService;
    let dotSessionStorageService: DotSessionStorageService;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotPaletteStore,
                DotSessionStorageService,
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKeyAsList: () => of([])
                    }
                },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotContentTypeService, useClass: MockContentTypeService },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        });
        dotPaletteStore = TestBed.inject(DotPaletteStore);
        paginatorService = TestBed.inject(PaginatorService);
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        dotESContentService = TestBed.inject(DotESContentService);
        dotSessionStorageService = TestBed.inject(DotSessionStorageService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    // Updaters
    it('should update filter', () => {
        dotPaletteStore.setFilter('test');
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.filter).toEqual('test');
        });
    });

    it('should update languageId', () => {
        dotPaletteStore.setLanguage('4');
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.languageId).toEqual('4');
            expect(data.filter).toEqual('');
            expect(data.viewContentlet).toEqual('contentlet:out');
        });
    });

    it('should update viewContentlet', () => {
        dotPaletteStore.setViewContentlet('in');
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.viewContentlet).toEqual('in');
        });
    });

    it('should update setLoading', () => {
        dotPaletteStore.setLoading();
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.loading).toEqual(true);
        });
    });

    it('should update setLoaded', () => {
        dotPaletteStore.setLoaded();
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.loading).toEqual(false);
        });
    });

    it('should update allowdContent', () => {
        const allowedContent = ['banner', 'contact', 'block editor'];
        dotPaletteStore.setAllowedContent(allowedContent);
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.allowedContent).toEqual(allowedContent);
        });
    });

    // Effects
    it('should load contentTypes to store', (done) => {
        jest.spyOn(dotContentTypeService, 'filterContentTypes').and.returnValues(
            of(SORTED_CONTENT_TYPE_MOCK)
        );
        jest.spyOn(dotContentTypeService, 'getContentTypes').and.returnValues(of([]));

        dotPaletteStore.loadContentTypes(['blog', 'banner']);
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentTypes).toEqual(SORTED_CONTENT_TYPE_MOCK);
            done();
        });
    });

    it("should load contentTypes and remove the hidden is the CONTENT_PALETTE_HIDDEN_CONTENT_TYPES is setted'", (done) => {
        jest.spyOn(dotContentTypeService, 'filterContentTypes').and.returnValues(
            of(SORTED_CONTENT_TYPE_MOCK)
        );
        jest.spyOn(dotContentTypeService, 'getContentTypes').and.returnValues(of([]));
        jest.spyOn(dotPropertiesService, 'getKeyAsList').mockReturnValue(of(['Form']));

        const expectedData = SORTED_CONTENT_TYPE_MOCK.filter((item) => item.variable !== 'Form');

        dotPaletteStore.loadContentTypes(['blog', 'banner']);
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentTypes).toEqual(expectedData);
            done();
        });
    });

    it('should load only widgets to store if allowedContent is empty', (done) => {
        jest.spyOn(dotContentTypeService, 'filterContentTypes').and.returnValues(of([]));
        jest.spyOn(dotContentTypeService, 'getContentTypes').and.returnValues(
            of(SORTED_CONTENT_TYPE_MOCK)
        );
        dotPaletteStore.loadContentTypes([]);
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentTypes).toEqual(SORTED_CONTENT_TYPE_MOCK);
            done();
        });

        expect(dotContentTypeService.filterContentTypes).not.toHaveBeenCalled();
        expect(dotContentTypeService.getContentTypes).toHaveBeenCalled();
    });

    it('should load Forms contentlets to store', (done) => {
        jest.spyOn(paginatorService, 'getWithOffset').mockReturnValue(of([contentletFormDataMock]));
        dotPaletteStore.loadContentlets('forms');

        expect(paginatorService.url).toBe('v1/contenttype');
        expect(paginatorService.paginationPerPage).toBe(25);
        expect(paginatorService.sortField).toBe('modDate');
        expect(paginatorService.sortOrder).toBe(1);

        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentlets).toEqual([
                contentletFormDataMock
            ] as unknown as DotCMSContentType[]);
            expect(data.filter).toEqual('');
            expect(data.loading).toEqual(false);
            expect(data.totalRecords).toEqual(paginatorService.totalRecords);
            done();
        });
    });

    it('should load Product contentlets to store', (done) => {
        jest.spyOn(dotESContentService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: [contentletProductDataMock] as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 20
            })
        );
        dotPaletteStore.loadContentlets('product');

        dotPaletteStore.vm$.subscribe((data) => {
            expect(dotESContentService.get).toHaveBeenCalledWith({
                itemsPerPage: 25,
                lang: '1',
                filter: '',
                offset: '0',
                query: '+contentType: product +deleted: false'
            });
            expect(data.contentlets).toEqual([
                contentletProductDataMock
            ] as unknown as DotCMSContentlet[]);
            expect(data.filter).toEqual('');
            expect(data.loading).toEqual(false);
            expect(data.totalRecords).toEqual(1); // changed due a filter the data in the store and the totalRecords now have the real amount of the array
            done();
        });
    });

    it('should set filter value in store', (done) => {
        jest.spyOn(dotESContentService, 'get').mockReturnValue(
            of({
                contentTook: 0,
                jsonObjectView: {
                    contentlets: [contentletProductDataMock] as unknown as DotCMSContentlet[]
                },
                queryTook: 1,
                resultsSize: 20
            })
        );
        dotPaletteStore.filterContentlets('Prod');
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.filter).toEqual('Prod');
            done();
        });
    });

    it('should filter contenttypes in stores', fakeAsync(() => {
        jest.spyOn(dotContentTypeService, 'filterContentTypes').mockReturnValue(of(responseData));
        jest.spyOn(dotContentTypeService, 'getContentTypes').mockReturnValue(of(responseData));

        const allowedContent = ['banner', 'blog'];
        const filter = 'blog';

        dotPaletteStore.setAllowedContent(allowedContent);
        dotPaletteStore.filterContentTypes(filter);

        tick(400);

        dotPaletteStore.vm$.subscribe((data) => expect(data.filter).toEqual(filter));

        expect(dotContentTypeService.filterContentTypes).toHaveBeenCalledWith(
            filter,
            allowedContent.join(',')
        );
        expect(dotContentTypeService.getContentTypes).toHaveBeenCalledWith({
            filter,
            page: 40,
            type: 'WIDGET'
        });
    }));

    it('should not call filterContentTypes is filter values es shoter than 3 caracteres', fakeAsync(() => {
        jest.spyOn(dotContentTypeService, 'filterContentTypes').mockReturnValue(of(responseData));
        jest.spyOn(dotContentTypeService, 'getContentTypes').mockReturnValue(of(responseData));

        const allowedContent = ['banner', 'blog'];
        const filter = 'bo';

        dotPaletteStore.setAllowedContent(allowedContent);
        dotPaletteStore.filterContentTypes(filter);

        tick(400);

        expect(dotContentTypeService.filterContentTypes).not.toHaveBeenCalled();
        expect(dotContentTypeService.getContentTypes).not.toHaveBeenCalled();
    }));

    describe('handle variant contentlets', () => {
        beforeEach(() => {
            jest.spyOn(dotSessionStorageService, 'getVariationId').mockReturnValue(VARIANT_ID_MOCK);
        });
        it('should remove the `DEFAULT` Contentlets and leave the copied', (done) => {
            jest.spyOn(dotESContentService, 'get').mockReturnValue(
                of({
                    contentTook: 0,
                    jsonObjectView: {
                        contentlets: [
                            ...ContentletWithDuplicatedMock,
                            ...NotDuplicatedContentletMock
                        ]
                    },
                    queryTook: 1,
                    resultsSize: 20
                })
            );

            dotPaletteStore.loadContentlets('');

            dotPaletteStore.vm$.subscribe(({ contentlets }) => {
                expect(contentlets.length).toEqual(2);

                const contentlet = contentlets[0] as DotCMSContentlet;
                expect(ContentletWithDuplicatedMock[0].inode).toEqual(contentlet.inode);
                done();
            });
        });
        it('should leave the created contentled in the variant', (done) => {
            jest.spyOn(dotESContentService, 'get').mockReturnValue(
                of({
                    contentTook: 0,
                    jsonObjectView: {
                        contentlets: [...NotDuplicatedContentletMock, ...NewVariantContentletMock]
                    },
                    queryTook: 1,
                    resultsSize: 20
                })
            );

            dotPaletteStore.loadContentlets('');

            dotPaletteStore.vm$.subscribe(({ contentlets }) => {
                expect(contentlets.length).toEqual(2);

                const contentletsStore = contentlets as DotCMSContentlet[];
                expect(NotDuplicatedContentletMock[0].inode).toEqual(contentletsStore[0].inode);
                expect(NewVariantContentletMock[0].inode).toEqual(contentletsStore[1].inode);
                done();
            });
        });

        it('should leave the created variant contentled and delete the `DEFAULT` Contentlets modified ', (done) => {
            jest.spyOn(dotESContentService, 'get').mockReturnValue(
                of({
                    contentTook: 0,
                    jsonObjectView: {
                        contentlets: [
                            ...ContentletWithDuplicatedMock,
                            ...NotDuplicatedContentletMock,
                            ...NewVariantContentletMock
                        ]
                    },
                    queryTook: 1,
                    resultsSize: 20
                })
            );

            dotPaletteStore.loadContentlets('');

            dotPaletteStore.vm$.subscribe(({ contentlets }) => {
                expect(contentlets.length).toEqual(3);

                const contentletsStore = contentlets as DotCMSContentlet[];
                expect(ContentletWithDuplicatedMock[0].inode).toEqual(contentletsStore[0].inode);
                expect(NotDuplicatedContentletMock[0].inode).toEqual(contentletsStore[1].inode);
                expect(NewVariantContentletMock[0].inode).toEqual(contentletsStore[2].inode);
                done();
            });
        });
    });
});
