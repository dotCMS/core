import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@dotcms/ui';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { By } from '@angular/platform-browser';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotFilterPipeModule } from '@pipes/dot-filter/dot-filter-pipe.module';
import { FormsModule } from '@angular/forms';
import { DotPaletteContentTypeComponent } from './dot-palette-content-type.component';
import { DotPaletteInputFilterModule } from '../dot-palette-input-filter/dot-palette-input-filter.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';

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
    template: ` <dot-palette-content-type [items]="items"></dot-palette-content-type> `
})
class TestHostComponent {
    @Input() items: any[];
    @Output() filter = new EventEmitter<any>();
}

@Injectable()
class MockDotContentletEditorService {
    setDraggedContentType = jasmine.createSpy('setDraggedContentType');
}

describe('DotPaletteContentTypeComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let dotContentletEditorService: DotContentletEditorService;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotPaletteContentTypeComponent],
            imports: [
                DotPipesModule,
                DotIconModule,
                DotFilterPipeModule,
                FormsModule,
                DotPaletteInputFilterModule,
                HttpClientTestingModule
            ],
            providers: [
                { provide: DotContentletEditorService, useClass: MockDotContentletEditorService },
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });

        fixtureHost = TestBed.createComponent(TestHostComponent);
        componentHost = fixtureHost.componentInstance;

        de = fixtureHost.debugElement.query(By.css('dot-palette-content-type'));
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

    it('should show empty state', () => {
        componentHost.items = [];
        fixtureHost.detectChanges();
        const emptyState = fixtureHost.debugElement.query(By.css('[data-testId="emptyState"]'));
        expect(emptyState).not.toBeNull();
    });

    it('should filter items on search', () => {
        componentHost.items = contentTypeDataMock;
        fixtureHost.detectChanges();
        const input = fixtureHost.debugElement.query(By.css('dot-palette-input-filter'));
        input.componentInstance.filter.emit('Product');
        fixtureHost.detectChanges();
        const contents = fixtureHost.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        expect(contents.length).toEqual(1);
    });

    it('should set Dragged ContentType on dragStart', () => {
        componentHost.items = contentTypeDataMock;
        fixtureHost.detectChanges();
        const content = fixtureHost.debugElement.query(By.css('[data-testId="paletteItem"]'));
        content.triggerEventHandler('dragstart', contentTypeDataMock[0]);
        expect(dotContentletEditorService.setDraggedContentType).toHaveBeenCalledOnceWith(
            contentTypeDataMock[0] as DotCMSContentType
        );
    });

    it('should emit event to show a specific contentlet', () => {
        componentHost.items = contentTypeDataMock;
        spyOn(de.componentInstance.selected, 'emit').and.callThrough();
        fixtureHost.detectChanges();
        const buttons = fixtureHost.debugElement.queryAll(By.css('[data-testId="paletteItem"]'));
        buttons[3].nativeElement.click();
        expect(de.componentInstance.itemsFiltered).toEqual(contentTypeDataMock);
        expect(de.componentInstance.selected.emit).toHaveBeenCalledWith('Text');
    });
});
