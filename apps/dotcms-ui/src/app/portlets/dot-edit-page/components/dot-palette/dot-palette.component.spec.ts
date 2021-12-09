import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPaletteComponent } from './dot-palette.component';
import { Component, DebugElement, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { dotcmsContentTypeBasicMock } from '@dotcms/app/test/dot-content-types.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

@Component({
    selector: 'dot-palette-content-type',
    template: ''
})
export class DotPaletteContentTypeMockComponent {
    @Input() items: any[];
    @Output() selected = new EventEmitter<any>();

    focusInputFilter() {}
}

@Component({
    selector: 'dot-palette-contentlets',
    template: ''
})
export class DotPaletteContentletsMockComponent {
    @Input() contentTypeVariable: string;
    @Input() languageId: string;
    @Output() back = new EventEmitter<any>();


    focusInputFilter() {}
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

describe('DotPaletteComponent', () => {
    let comp: DotPaletteComponent;
    let fixture: ComponentFixture<DotPaletteComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotPaletteComponent,
                DotPaletteContentletsMockComponent,
                DotPaletteContentTypeMockComponent
            ],
            imports: [HttpClientTestingModule, NoopAnimationsModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }]
        });

        fixture = TestBed.createComponent(DotPaletteComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    });

    it('should dot-palette-content-type have items assigned', () => {
        comp.items = [itemMock];
        fixture.detectChanges();
        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp.componentInstance.items).toEqual([itemMock]);
    });

    it('should change view to contentlets and set Content Type Variable on contentlets palette view', async () => {
        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        contentTypeComp.triggerEventHandler('selected', 'Blog');
        fixture.detectChanges();
        await fixture.whenStable();
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        expect(contentTypeComp.nativeElement.style.transform).toEqual('translateX(-100%)');
        expect(contentContentletsComp.nativeElement.style.transform).toEqual('translateX(-100%)');
        expect(contentContentletsComp.componentInstance.contentTypeVariable).toEqual('Blog');
    });

    it('should change view to content type and unset Content Type Variable on contentlets palette view', async () => {
        const contentContentletsComp = fixture.debugElement.query(
            By.css('dot-palette-contentlets')
        );
        contentContentletsComp.triggerEventHandler('back', '');
        comp.languageId = '2';
        fixture.detectChanges();
        await fixture.whenStable();
        const contentTypeComp = fixture.debugElement.query(By.css('dot-palette-content-type'));
        expect(contentTypeComp.nativeElement.style.transform).toEqual('translateX(0px)');
        expect(contentContentletsComp.componentInstance.languageId).toEqual('2');
        expect(contentContentletsComp.nativeElement.style.transform).toEqual('translateX(100%)');
    });
});
