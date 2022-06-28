import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { DotPaletteStore } from './dot-palette.store';
import { Injectable } from '@angular/core';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { PaginatorService } from '@dotcms/app/api/services/paginator';
import { contentTypeDataMock } from '../dot-palette-content-type/dot-palette-content-type.component.spec';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { ESContent } from '../../../../../shared/models/dot-es-content/dot-es-content.model';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

import {
    contentletFormDataMock,
    contentletProductDataMock
} from '../dot-palette-contentlets/dot-palette-contentlets.component.spec';

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

describe('DotPaletteStore', () => {
    let dotPaletteStore: DotPaletteStore;
    let paginatorService: PaginatorService;
    let dotContentTypeService: DotContentTypeService;
    let dotESContentService: DotESContentService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotPaletteStore,
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotContentTypeService, useClass: MockContentTypeService },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        });
        dotPaletteStore = TestBed.inject(DotPaletteStore);
        paginatorService = TestBed.inject(PaginatorService);
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        dotESContentService = TestBed.inject(DotESContentService);
    });

    // Updaters
    it('should update filter', () => {
        dotPaletteStore.setFilter('test');
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.filter).toEqual('test');
        });
    });

    it('should update languageId', () => {
        dotPaletteStore.setLanguageId('1');
        dotPaletteStore.state$.subscribe((data) => {
            expect(data.languageId).toEqual('1');
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
        const sortedDataMock = contentTypeDataMock.sort((a, b) => a.name.localeCompare(b.name));
        spyOn(dotContentTypeService, 'filterContentTypes').and.returnValues(
            of(sortedDataMock as DotCMSContentType[])
        );
        spyOn(dotContentTypeService, 'getContentTypes').and.returnValues(of([]));
        dotPaletteStore.loadContentTypes(['blog', 'banner']);
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentTypes).toEqual(sortedDataMock as DotCMSContentType[]);
            done();
        });
    });

    it('should load inly widgets to store if allowedContent is empty', (done) => {
        const sortedDataMock = contentTypeDataMock.sort((a, b) => a.name.localeCompare(b.name));
        spyOn(dotContentTypeService, 'filterContentTypes').and.returnValues(of([]));
        spyOn(dotContentTypeService, 'getContentTypes').and.returnValues(of(sortedDataMock as DotCMSContentType[]));
        dotPaletteStore.loadContentTypes([]);
        dotPaletteStore.vm$.subscribe((data) => {
            expect(data.contentTypes).toEqual(sortedDataMock as DotCMSContentType[]);
            done();
        });

        expect(dotContentTypeService.filterContentTypes).not.toHaveBeenCalled();
        expect(dotContentTypeService.getContentTypes).toHaveBeenCalled();
    });

    it('should load Forms contentlets to store', (done) => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([contentletFormDataMock]));
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
        spyOn(dotESContentService, 'get').and.returnValue(
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
                query: '+contentType: product'
            });
            expect(data.contentlets).toEqual([
                contentletProductDataMock
            ] as unknown as DotCMSContentlet[]);
            expect(data.filter).toEqual('');
            expect(data.loading).toEqual(false);
            expect(data.totalRecords).toEqual(20);
            done();
        });
    });

    it('should set filter value in store', (done) => {
        spyOn(dotESContentService, 'get').and.returnValue(
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
        spyOn(dotContentTypeService, 'filterContentTypes').and.returnValue(of(responseData));
        spyOn(dotContentTypeService, 'getContentTypes').and.returnValue(of(responseData));

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
        spyOn(dotContentTypeService, 'filterContentTypes').and.returnValue(of(responseData));
        spyOn(dotContentTypeService, 'getContentTypes').and.returnValue(of(responseData));

        const allowedContent = ['banner', 'blog'];
        const filter = 'bo';

        dotPaletteStore.setAllowedContent(allowedContent);
        dotPaletteStore.filterContentTypes(filter);

        tick(400);

        expect(dotContentTypeService.filterContentTypes).not.toHaveBeenCalled();
        expect(dotContentTypeService.getContentTypes).not.toHaveBeenCalled();
    }));
});