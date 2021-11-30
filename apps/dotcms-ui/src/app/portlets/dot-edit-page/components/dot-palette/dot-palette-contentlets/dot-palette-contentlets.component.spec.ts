import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule, DotSpinnerModule } from '@dotcms/ui';
import { By } from '@angular/platform-browser';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotFilterPipeModule } from '@pipes/dot-filter/dot-filter-pipe.module';
import { FormsModule } from '@angular/forms';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotPaletteContentletsComponent } from './dot-palette-contentlets.component';
import { Observable, of } from 'rxjs';
import { PaginatorService } from '@services/paginator';
import { DotESContentService } from '@services/dot-es-content/dot-es-content.service';
import { PaginatorModule } from 'primeng/paginator';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

const formData = {
    baseType: 'FORM',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableFormContentType',
    defaultType: false,
    description: 'General Contact Form',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    iDate: 1563384216000,
    icon: 'person_add',
    id: '897cf4a9-171a-4204-accb-c1b498c813fe',
    layout: [],
    modDate: 1637624574000,
    multilingualable: false,
    nEntries: 0,
    name: 'Contact',
    sortOrder: 0,
    system: false,
    variable: 'Contact',
    versionable: true,
    workflows: []
};

const productData = {
    baseType: 'CONTENT',
    contentType: 'Product',
    contentTypeIcon: 'inventory',
    description: "<p>The Patagonia Women's Retro Pile...",
    hasTitleImage: true,
    identifier: 'c4ce9da8-f97b-4d43-b52d-99893f57e68a',
    image: '/dA/c4ce9da8-f97b-4d43-b52d-99893f57e68a/image/women-vest.jpg',
    inode: 'e95c60b6-138c-4f6b-b317-0af53f0b0fd4',
    modDate: '2020-09-02 16:45:15.569',
    stInode: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
    title: "Patagonia Women's Retro Pile Fleece Vest",
    url: '/content.25b24d5b-1eb3-4cf0-9fe5-7739e442ff58',
    __icon__: 'contentIcon'
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-palette-contentlets
            [languageId]="languageId"
            [contentTypeVariable]="contentTypeVariable"
        ></dot-palette-contentlets>
    `
})
class TestHostComponent {
    @Input() languageId: string;
    @Input() contentTypeVariable: string;
    @Output() filter = new EventEmitter<any>();
}

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jasmine.createSpy('setDraggedContentType');
}

@Injectable()
class MockPaginatorService {
    url: string;
    paginationPerPage = 10;
    maxLinksPage = 5;
    sortField: string;
    sortOrder: string;
    totalRecords = 40;

    setExtraParams(): void {}

    public getWithOffset(): Observable<any[]> {
        return null;
    }
}

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 15;
    totalRecords = 20;

    public get(): Observable<any[]> {
        return null;
    }
}

@Component({
    selector: 'dot-contentlet-icon',
    template: ''
})
export class DotContentletIconMockComponent {
    @Input() icon: string;
    @Input() size: string;

    constructor() {}
}

describe('DotPaletteContentletsComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let dotContentletEditorService: DotContentletEditorService;
    let paginatorService: PaginatorService;
    let paginatorESService: DotESContentService;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestHostComponent,
                DotPaletteContentletsComponent,
                DotContentletIconMockComponent
            ],
            imports: [
                DotPipesModule,
                DotSpinnerModule,
                DotIconModule,
                DotFilterPipeModule,
                FormsModule,
                DotPaletteInputFilterModule,
                HttpClientTestingModule,
                PaginatorModule
            ],
            providers: [
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotESContentService, useClass: MockESPaginatorService },

                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });

        fixtureHost = TestBed.createComponent(TestHostComponent);
        componentHost = fixtureHost.componentInstance;

        de = fixtureHost.debugElement.query(By.css('dot-palette-contentlets'));
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        paginatorService = de.injector.get(PaginatorService);
        paginatorESService = de.injector.get(DotESContentService);
        spyOn(paginatorService, 'setExtraParams').and.callThrough();

        fixtureHost.detectChanges();
    });

    it('should load initial params correctly and loading Forms via PaginatorService', async () => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([formData]));
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'forms';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const contentletIcon = fixtureHost.debugElement.query(By.css('dot-contentlet-icon'));

        expect(paginatorService.url).toBe('v1/contenttype');
        expect(paginatorService.paginationPerPage).toBe(15);
        expect(paginatorService.sortField).toBe('modDate');
        expect(paginatorService.sortOrder).toBe(1);
        expect(paginatorService.setExtraParams).toHaveBeenCalledWith('type', 'Form');
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(de.componentInstance.items.length).toBe(1);
        expect(de.componentInstance.hideNoResults).toBe(true);
        expect(contentletIcon.componentInstance.icon).toBe(formData.icon);
        expect(contentletIcon.componentInstance.size).toBe('45px');
    });

    it('should load intital Product data via DotESContent', async () => {
        spyOn(paginatorESService, 'get').and.returnValue(
            <any>of({
                contentTook: 0,
                jsonObjectView: { contentlets: [productData] },
                queryTook: 1,
                resultsSize: 20
            })
        );
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'Product';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const contentletImg = fixtureHost.debugElement.query(
            By.css('[data-testId="paletteItem"] img')
        );

        expect(paginatorESService.get).toHaveBeenCalledWith({
            itemsPerPage: 15,
            lang: '1',
            filter: '',
            offset: '0',
            query: `+contentType: ${productData.contentType}`
        });
        expect(de.componentInstance.items.length).toBe(1);
        expect(de.componentInstance.hideNoResults).toBe(true);
        expect(contentletImg.nativeElement.src).toContain(
            `/dA/${productData.inode}/titleImage/48w`
        );
    });

    it('should load with No Results data', async () => {
        spyOn(paginatorESService, 'get').and.returnValue(
            <any>of({
                contentTook: 0,
                jsonObjectView: { contentlets: [] },
                queryTook: 1,
                resultsSize: 0
            })
        );
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'Product';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const noResultsContainer = fixtureHost.debugElement.query(
            By.css('[data-testId="emptyState"]')
        );
        expect(noResultsContainer).toBeTruthy();
    });

    it('should paginate products data via DotESContent', async () => {
        let productsArray = [];
        for (let index = 0; index < 20; index++) {
            productsArray.push(productData);
        }
        spyOn(paginatorESService, 'get').and.returnValue(
            <any>of({
                contentTook: 0,
                jsonObjectView: { contentlets: productsArray },
                queryTook: 1,
                resultsSize: 20
            })
        );
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'Product';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const paginatorContainer = fixtureHost.debugElement.query(By.css('p-paginator'));

        expect(paginatorContainer).toBeTruthy();
        expect(paginatorContainer.componentInstance.rows).toBe(15);
        expect(paginatorContainer.componentInstance.totalRecords).toBe(20);
        expect(paginatorContainer.componentInstance.showFirstLastIcon).toBe(false);
        expect(paginatorContainer.componentInstance.pageLinkSize).toBe('2');

        paginatorContainer.componentInstance.onPageChange.emit({ first: 15 });

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        expect(paginatorESService.get).toHaveBeenCalledWith({
            itemsPerPage: 15,
            lang: '1',
            filter: '',
            offset: '15',
            query: `+contentType: ${productData.contentType}`
        });
    });

    it('should paginate forms data via PaginatorService', async () => {
        let formsArray = [];
        for (let index = 0; index < 20; index++) {
            formsArray.push(formData);
        }
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of(formsArray));
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'forms';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const paginatorContainer = fixtureHost.debugElement.query(By.css('p-paginator'));
        expect(paginatorContainer).toBeTruthy();
        paginatorContainer.componentInstance.onPageChange.emit({ first: 15 });

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(15);
    });

    it('should set Dragged ContentType on dragStart', async () => {
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([formData]));
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'forms';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const content = fixtureHost.debugElement.query(By.css('[data-testId="paletteItem"]'));
        content.triggerEventHandler('dragstart', productData);

        expect(dotContentletEditorService.setDraggedContentType).toHaveBeenCalledOnceWith(
            (<any>formData) as DotCMSContentlet
        );
    });

    it('should go back to show Content Type components', async () => {
        spyOn(paginatorESService, 'get').and.returnValue(
            <any>of({
                contentTook: 0,
                jsonObjectView: { contentlets: [productData] },
                queryTook: 1,
                resultsSize: 2
            })
        );
        spyOn(de.componentInstance.hide, 'emit').and.callThrough();
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'Product';

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.goBack.emit();

        expect(filterComp.componentInstance.goBackBtn).toBe(true);
        expect(de.componentInstance.items).toEqual(null);
        expect(de.componentInstance.filter).toEqual('');
        expect(de.componentInstance.hide.emit).toHaveBeenCalled();
    });

    it('should filter Product items on search via DotESContent', async () => {
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'Product';
        spyOn(paginatorESService, 'get').and.returnValue(
            of({
                contentTook: 0,
                jsonObjectView: { contentlets: [] },
                queryTook: 1,
                resultsSize: 2
            })
        );

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.filter.emit('test');

        fixtureHost.detectChanges();

        expect(paginatorESService.get).toHaveBeenCalledWith({
            itemsPerPage: 15,
            lang: '1',
            filter: 'test',
            offset: '0',
            query: `+contentType: ${productData.contentType}`
        });
    });

    it('should filter Forms items on search via PaginatorService', async () => {
        componentHost.languageId = '1';
        componentHost.contentTypeVariable = 'forms';
        spyOn(paginatorService, 'getWithOffset').and.returnValue(of([]));

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.filter.emit('test');

        fixtureHost.detectChanges();

        expect(paginatorService.searchParam).toBe('variable');
        expect(paginatorService.filter).toBe('test');
        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
    });
});
