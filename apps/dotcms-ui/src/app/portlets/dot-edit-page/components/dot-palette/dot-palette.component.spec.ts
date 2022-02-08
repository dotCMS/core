/* eslint-disable @typescript-eslint/no-explicit-any */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotPaletteComponent } from './dot-palette.component';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { dotcmsContentTypeBasicMock } from '@dotcms/app/test/dot-content-types.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of } from 'rxjs';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { PaginatorService } from '@dotcms/app/api/services/paginator';
import { DotPaletteStore, LoadingState } from './store/dot-palette.store';
import { contentletProductDataMock } from './dot-palette-contentlets/dot-palette-contentlets.component.spec';

@Component({
    selector: 'dot-palette-content-type',
    template: ''
})
export class DotPaletteContentTypeMockComponent {
    @Input() items: any[];
    @Output() selected = new EventEmitter<any>();

    focusInputFilter() {
        //
    }
}

@Component({
    selector: 'dot-palette-contentlets',
    template: ''
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

    setExtraParams(): void {}

    public getWithOffset(): Observable<any[]> {
        return null;
    }
}

const storeMock = jasmine.createSpyObj(
    'DotPaletteStore',
    [
        'getContentletsData',
        'setFilter',
        'setLanguageId',
        'setViewContentlet',
        'setLoading',
        'setLoaded',
        'loadContentTypes',
        'filterContentlets',
        'loadContentlets',
        'switchView'
    ],
    {
        vm$: of({
            contentlets: [contentletProductDataMock],
            contentTypes: [itemMock],
            filter: '',
            languageId: '1',
            totalRecords: 20,
            viewContentlet: 'contentlet:out',
            callState: LoadingState.LOADED
        })
    }
);

describe('DotPaletteComponent', () => {
    let comp: DotPaletteComponent;
    let fixture: ComponentFixture<DotPaletteComponent>;
    let de: DebugElement;
    let store: DotPaletteStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotPaletteComponent,
                DotPaletteContentletsMockComponent,
                DotPaletteContentTypeMockComponent
            ],
            imports: [HttpClientTestingModule, NoopAnimationsModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: PaginatorService, useClass: MockPaginatorService },
                { provide: DotESContentService, useClass: MockESPaginatorService }
            ]
        });
        TestBed.overrideProvider(DotPaletteStore, { useValue: storeMock });
        store = TestBed.inject(DotPaletteStore);

        fixture = TestBed.createComponent(DotPaletteComponent);
        comp = fixture.componentInstance;
        comp.items = [itemMock];
        fixture.detectChanges();
    });

    it('should dot-palette-content-type have items assigned', () => {
        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp.componentInstance.items).toEqual([itemMock]);
    });

    it('should change view to contentlets and set viewContentlet Variable on contentlets palette view', async () => {
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        contentTypeComp.triggerEventHandler('selected', 'Blog');

        fixture.detectChanges();
        await fixture.whenStable();

        const wrapper = fixture.debugElement.query(By.css('[data-testid="wrapper"]'));
        expect(wrapper.nativeElement.style.transform).toEqual('translateX(0%)');
        expect(store.switchView).toHaveBeenCalledWith('Blog');
        expect(contentContentletsComp.componentInstance.totalRecords).toBe(20);
        expect(contentContentletsComp.componentInstance.items).toEqual([contentletProductDataMock]);
    });

    it('should change view to content type and unset viewContentlet Variable on contentlets palette view', async () => {
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        contentContentletsComp.triggerEventHandler('back', '');

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.switchView).toHaveBeenCalledWith(undefined);
    });

    it('should set value on store on filtering event', async () => {
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        contentContentletsComp.triggerEventHandler('filter', 'test');

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.filterContentlets).toHaveBeenCalledWith('test');
    });

    it('should set value on store on paginate event', async () => {
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        contentContentletsComp.triggerEventHandler('paginate', { first: 20 });

        fixture.detectChanges();
        await fixture.whenStable();

        expect(store.getContentletsData).toHaveBeenCalledWith({ first: 20 });
    });
});
