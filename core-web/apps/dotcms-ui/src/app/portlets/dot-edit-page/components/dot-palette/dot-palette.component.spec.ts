/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotESContentService, PaginatorService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { contentletProductDataMock } from './dot-palette-contentlets/dot-palette-contentlets.component.spec';
import { DotPaletteComponent } from './dot-palette.component';
import { DotPaletteStore } from './store/dot-palette.store';

import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jest.fn();
}

@Component({
    selector: 'dot-palette-content-type',
    template: '',
    standalone: false
})
export class DotPaletteContentTypeMockComponent {
    @Input() items: any[];
    @Input() loading: any[];
    @Input() viewContentlet: any[];
    @Output() selected = new EventEmitter<any>();
    @Output() filter = new EventEmitter<string>();

    focusInputFilter() {
        //
    }
}

@Component({
    selector: 'dot-palette-contentlets',
    template: '',
    standalone: false
})
export class DotPaletteContentletsMockComponent {
    @Input() items: string;
    @Input() loading: boolean;
    @Input() totalRecords: number;
    @Output() back = new EventEmitter<any>();
    @Output() filter = new EventEmitter<any>();
    @Output() paginate = new EventEmitter<any>();

    focusInputFilter() {
        //
    }
}

const itemMock = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    id: '1234567890',
    name: 'Nuevo',
    variable: 'Nuevo',
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: null,
    owner: '123',
    system: false
};

@Injectable()
class MockESPaginatorService {
    paginationPerPage = 15;
    totalRecords = 20;

    public get(): Observable<any[]> {
        return null;
    }
}

@Injectable()
class MockPaginatorService {
    url: string;
    paginationPerPage = 10;
    maxLinksPage = 5;
    sortField: string;
    sortOrder: string;
    totalRecords = 40;

    setExtraParams(): void {
        /* */
    }

    public getWithOffset(): Observable<any[]> {
        return null;
    }
}

const storeMock = {
    getContentletsData: jest.fn(),
    getContenttypesData: jest.fn(),
    setAllowedContent: jest.fn(),
    setFilter: jest.fn(),
    setLanguageId: jest.fn(),
    setViewContentlet: jest.fn(),
    setLoading: jest.fn(),
    setLoaded: jest.fn(),
    loadContentTypes: jest.fn(),
    filterContentlets: jest.fn(),
    filterContentTypes: jest.fn(),
    loadContentlets: jest.fn(),
    switchView: jest.fn(),
    switchLanguage: jest.fn(),
    vm$: of({
        contentlets: [contentletProductDataMock],
        contentTypes: [itemMock],
        allowedContent: null,
        filter: '',
        languageId: '1',
        loading: false,
        totalRecords: 20,
        viewContentlet: 'contentlet:out',
        callState: ComponentStatus.LOADED
    })
};

describe('DotPaletteComponent', () => {
    let comp: DotPaletteComponent;
    let fixture: ComponentFixture<DotPaletteComponent>;
    let store: DotPaletteStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPaletteContentletsMockComponent, DotPaletteContentTypeMockComponent],
            imports: [DotPaletteComponent, HttpClientTestingModule, NoopAnimationsModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotESContentService, useClass: MockESPaginatorService },
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService }
            ]
        });
        TestBed.overrideProvider(DotPaletteStore, { useValue: storeMock });
        store = TestBed.inject(DotPaletteStore);

        fixture = TestBed.createComponent(DotPaletteComponent);
        comp = fixture.componentInstance;
        fixture.detectChanges();

        // Clear all mocks before each test
        jest.clearAllMocks();
    });

    it('should dot-palette-content-type have items assigned', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp).toBeTruthy();
        expect(contentTypeComp.componentInstance.items).toEqual([itemMock]);
        expect(contentTypeComp.componentInstance.loading).toBeFalsy();
        expect(contentTypeComp.componentInstance.viewContentlet).toEqual('contentlet:out');
    });

    it('should change view to contentlets and set viewContentlet Variable on contentlets palette view', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp).toBeTruthy();
        contentTypeComp.triggerEventHandler('selected', 'Blog');

        fixture.detectChanges();
        await fixture.whenStable();

        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        expect(contentContentletsComp).toBeTruthy();

        const wrapper = fixture.debugElement.query(By.css('[data-testid="wrapper"]'));
        expect(wrapper.nativeElement.style.transform).toEqual('translateX(0%)');
        expect(store.switchView).toHaveBeenCalledWith('Blog');
        expect(store.switchView).toHaveBeenCalledTimes(1);
        expect(contentContentletsComp.componentInstance.totalRecords).toBe(20);
        expect(contentContentletsComp.componentInstance.items).toEqual([contentletProductDataMock]);
    });

    it('should call filterContentTypes when content type compenent emits filter event', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp).toBeTruthy();
        contentTypeComp.triggerEventHandler('filter', 'Blog');

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.filterContentTypes).toHaveBeenCalledWith('Blog');
        expect(store.filterContentTypes).toHaveBeenCalledTimes(1);
    });

    it('should change view to content type and unset viewContentlet Variable on contentlets palette view', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        expect(contentContentletsComp).toBeTruthy();
        contentContentletsComp.triggerEventHandler('back', '');

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.switchView).toHaveBeenCalledWith(undefined);
        expect(store.switchView).toHaveBeenCalledTimes(1);
    });

    it('should set value on store on filtering event', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        expect(contentContentletsComp).toBeTruthy();
        contentContentletsComp.triggerEventHandler('filter', 'test');

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.filterContentlets).toHaveBeenCalledWith('test');
        expect(store.filterContentlets).toHaveBeenCalledTimes(1);
    });

    it('should set value on store on paginate event', async () => {
        fixture.detectChanges();
        await fixture.whenStable();

        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        expect(contentContentletsComp).toBeTruthy();
        contentContentletsComp.triggerEventHandler('paginate', { first: 20 });

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.getContentletsData).toHaveBeenCalledWith({ first: 20 });
        expect(store.getContentletsData).toHaveBeenCalledTimes(1);
    });

    it('should set allowedContent', async () => {
        const allowedContent = ['persona', 'banner', 'contact'];
        comp.allowedContent = allowedContent;

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.setAllowedContent).toHaveBeenCalledWith(allowedContent);
        expect(store.setAllowedContent).toHaveBeenCalledTimes(1);
    });

    it('should switch language', async () => {
        comp.languageId = '2';

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.switchLanguage).toHaveBeenCalledWith('2');
        expect(store.switchLanguage).toHaveBeenCalledTimes(1);
    });
});
