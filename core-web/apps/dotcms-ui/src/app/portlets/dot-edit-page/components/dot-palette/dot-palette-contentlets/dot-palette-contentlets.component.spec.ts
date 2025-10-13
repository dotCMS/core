/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { LazyLoadEvent } from 'primeng/api';
import { PaginatorModule } from 'primeng/paginator';

import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe, DotSpinnerComponent } from '@dotcms/ui';

import { DotPaletteContentletsComponent } from './dot-palette-contentlets.component';

import { DotContentletEditorService } from '../../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotFilterPipeModule } from '../../../../../view/pipes/dot-filter/dot-filter-pipe.module';
import { DotPaletteInputFilterComponent } from '../dot-palette-input-filter/dot-palette-input-filter.component';

export const contentletFormDataMock = {
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

export const contentletProductDataMock = {
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
            [items]="items"
            [loading]="loading"
            [totalRecords]="totalRecords"></dot-palette-contentlets>
    `,
    standalone: false
})
class TestHostComponent {
    @Input() items: DotCMSContentlet[];
    @Input() loading: boolean;
    @Input() totalRecords: number;

    @Output() back = new EventEmitter();
    @Output() filter = new EventEmitter<string>();
    @Output() paginate = new EventEmitter<LazyLoadEvent>();
}

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jest.fn();
}

@Component({
    selector: 'dot-contentlet-icon',
    template: '',
    standalone: false
})
export class DotContentletIconMockComponent {
    @Input() icon: string;
    @Input() size: string;
}

describe('DotPaletteContentletsComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotPaletteContentletsComponent;
    let dotContentletEditorService: DotContentletEditorService;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotContentletIconMockComponent],
            imports: [
                DotPaletteContentletsComponent,
                DotMessagePipe,
                DotSpinnerComponent,
                DotIconComponent,
                DotFilterPipeModule,
                FormsModule,
                DotPaletteInputFilterComponent,
                HttpClientTestingModule,
                PaginatorModule
            ],
            providers: [
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService },
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });

        fixtureHost = TestBed.createComponent(TestHostComponent);
        componentHost = fixtureHost.componentInstance;

        de = fixtureHost.debugElement.query(By.css('dot-palette-contentlets'));
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        component = de.componentInstance;

        fixtureHost.detectChanges();
    });

    it('should load initial params correctly with contentlets', async () => {
        componentHost.items = [contentletProductDataMock] as unknown as DotCMSContentlet[];
        componentHost.loading = false;
        componentHost.totalRecords = 10;

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const contentletImg = fixtureHost.debugElement.query(
            By.css('[data-testId="paletteItem"] img')
        );
        expect(de.componentInstance.items.length).toBe(1);
        expect(contentletImg.nativeElement.src).toContain(
            `/dA/${contentletProductDataMock.inode}/titleImage/500w/50q`
        );
    });

    it('should load with No Results data', async () => {
        componentHost.items = [] as unknown as DotCMSContentlet[];
        componentHost.loading = false;
        componentHost.totalRecords = 0;

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const noResultsContainer = fixtureHost.debugElement.query(
            By.css('[data-testId="emptyState"]')
        );
        expect(noResultsContainer).toBeTruthy();
    });

    it('should emit paginate event', async () => {
        jest.spyOn(component.paginate, 'emit');
        const productsArray = [];
        for (let index = 0; index < 30; index++) {
            productsArray.push(contentletProductDataMock);
        }

        componentHost.items = [productsArray] as unknown as DotCMSContentlet[];
        componentHost.loading = false;
        componentHost.totalRecords = 30;

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const paginatorContainer = fixtureHost.debugElement.query(By.css('p-paginator'));

        expect(paginatorContainer).toBeTruthy();
        expect(paginatorContainer.componentInstance.rows).toBe(25);
        expect(paginatorContainer.componentInstance.totalRecords).toBe(30);
        expect(paginatorContainer.componentInstance.showFirstLastIcon).toBe(false);
        expect(paginatorContainer.componentInstance.pageLinkSize).toBe(2);

        paginatorContainer.componentInstance.onPageChange.emit({ first: 25 });

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        expect(component.paginate.emit).toHaveBeenCalledWith({
            first: 25
        });
    });

    it('should emit go back', async () => {
        jest.spyOn(component.back, 'emit');

        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.goBack.emit();

        expect(filterComp.componentInstance.goBackBtn).toBe(true);
        expect(component.back.emit).toHaveBeenCalled();
    });

    it('should set Dragged ContentType on dragStart', async () => {
        componentHost.items = [contentletProductDataMock] as unknown as DotCMSContentlet[];
        componentHost.loading = false;
        componentHost.totalRecords = 10;
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const content = fixtureHost.debugElement.query(By.css('[data-testId="paletteItem"]'));
        content.triggerEventHandler('dragstart', contentletProductDataMock);

        expect(dotContentletEditorService.setDraggedContentType).toHaveBeenCalledWith(
            (<any>contentletProductDataMock) as DotCMSContentlet
        );
    });

    it('should filter Product item', async () => {
        jest.spyOn(component.filter, 'emit');
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.filter.emit('test');

        fixtureHost.detectChanges();

        expect(component.filter.emit).toHaveBeenCalledWith('test');
        expect(component.filter.emit).toHaveBeenCalledTimes(1);
    });
});
