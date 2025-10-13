/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPaletteContentTypeComponent } from './dot-palette-content-type.component';

import { DotContentletEditorService } from '../../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotFilterPipeModule } from '../../../../../view/pipes/dot-filter/dot-filter-pipe.module';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';

export const contentTypeDataMock = [
    {
        baseType: 'Product',
        clazz: '',
        defaultType: false,
        icon: 'cloud',
        id: 'a1661fbc-9e84-4c00-bd62-76d633170da3',
        name: 'Product',
        variable: 'Product'
    },
    {
        baseType: 'Blog',
        clazz: '',
        defaultType: false,
        icon: 'alt_route',
        id: '799f176a-d32e-4844-a07c-1b5fcd107578',
        name: 'Blog',
        variable: 'Blog'
    },
    {
        baseType: 'Form',
        clazz: '',
        defaultType: false,
        icon: 'cloud',
        id: '897cf4a9-171a-4204-accb-c1b498c813fe',
        name: 'Contact',
        variable: 'Form'
    },
    {
        baseType: 'Text',
        clazz: '',
        defaultType: false,
        icon: 'person',
        id: '6044a806-f462-4977-a353-57539eac2a2c',
        name: 'Long name Blog Comment',
        variable: 'Text'
    }
];

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-palette-content-type
            [items]="items"
            [loading]="loading"
            [viewContentlet]="viewContentlet"></dot-palette-content-type>
    `,
    standalone: false
})
class TestHostComponent {
    @Input() items: any[];
    @Input() loading: boolean;
    @Input() viewContentlet: string;
    @Output() filter = new EventEmitter<any>();
}

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jest.fn();
}

describe('DotPaletteContentTypeComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let dotContentletEditorService: DotContentletEditorService;
    let de: DebugElement;
    let comp: DotPaletteContentTypeComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotPaletteContentTypeComponent,
                DotIconComponent,
                DotSafeHtmlPipe,
                DotMessagePipe,
                DotFilterPipeModule,
                FormsModule,
                DotPaletteInputFilterModule,
                HttpClientTestingModule
            ],
            providers: [
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService },
                {
                    provide: CoreWebService,
                    useValue: { request: jest.fn().mockReturnValue(of({})) }
                }
            ]
        });

        fixtureHost = TestBed.createComponent(TestHostComponent);
        componentHost = fixtureHost.componentInstance;

        de = fixtureHost.debugElement.query(By.css('dot-palette-content-type'));
        comp = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        fixtureHost.detectChanges();
    });

    it('should list items correctly', () => {
        componentHost.items = contentTypeDataMock;
        fixtureHost.detectChanges();
        const contents = fixtureHost.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        expect(contents.length).toEqual(4);
        expect(contents[0].nativeElement.draggable).toEqual(true);
    });

    it('should show empty state', async () => {
        componentHost.items = [];
        componentHost.loading = false;
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();
        const emptyState = fixtureHost.debugElement.query(By.css('[data-testId="emptyState"]'));
        expect(emptyState).not.toBeNull();
    });

    it('should show loading state', async () => {
        componentHost.items = [];
        componentHost.loading = true;
        componentHost.viewContentlet = 'contentlet:out';
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();
        const loading = fixtureHost.debugElement.query(By.css('dot-spinner'));
        expect(loading).not.toBeNull();
    });

    it('should not show loading state when switching view', async () => {
        componentHost.items = [];
        componentHost.loading = true;
        componentHost.viewContentlet = 'contentlet:in';
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();
        const loading = fixtureHost.debugElement.query(By.css('dot-spinner'));
        expect(loading).toBeNull();
    });

    it('should filter items on search', async () => {
        jest.spyOn(comp.filter, 'emit');
        fixtureHost.detectChanges();
        await fixtureHost.whenStable();

        const filterComp = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        filterComp.componentInstance.filter.emit('test');

        fixtureHost.detectChanges();

        expect(comp.filter.emit).toHaveBeenCalledWith('test');
        expect(comp.filter.emit).toHaveBeenCalledTimes(1);
    });

    it('should set Dragged ContentType on dragStart', () => {
        componentHost.items = contentTypeDataMock;
        fixtureHost.detectChanges();
        const content = fixtureHost.debugElement.query(By.css('[data-testId="paletteItem"]'));
        content.triggerEventHandler('dragstart', contentTypeDataMock[0]);
        expect(dotContentletEditorService.setDraggedContentType).toHaveBeenCalledWith(
            contentTypeDataMock[0] as DotCMSContentType
        );
    });

    it('should emit event to show a specific contentlet', () => {
        componentHost.items = contentTypeDataMock;
        jest.spyOn(comp.selected, 'emit');
        fixtureHost.detectChanges();
        const buttons = fixtureHost.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        const label = buttons[0].nativeElement.querySelector('p').textContent.trim();
        buttons[0].nativeElement.click();
        expect(comp.items).toEqual(contentTypeDataMock as DotCMSContentType[]);
        expect(comp.selected.emit).toHaveBeenCalledWith(label);
        expect(comp.selected.emit).toHaveBeenCalledTimes(1);
    });
});
