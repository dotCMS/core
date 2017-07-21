import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldDragabbleItemComponent } from './content-type-field-dragabble-item.component';
import { By } from '@angular/platform-browser';
import { Field } from '../';

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypesFieldDragabbleItemComponent;
    let fixture: ComponentFixture<ContentTypesFieldDragabbleItemComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesFieldDragabbleItemComponent
            ],
            imports: [
            ],
            providers: [
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldDragabbleItemComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should has a span', () => {
        let field =  {
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        let span = de.query(By.css('span'));
        expect(span).not.toBeNull();
        expect(span.nativeElement.textContent).toEqual(field.name);
    });

    it('should has a remove button', fakeAsync(() => {
        let field =  {
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        let button = de.query(By.css('button'));
        expect(button).not.toBeNull();
        expect('fa-remove').toEqual(button.attributes['icon']);

        let resp: Field;
        comp.remove.subscribe( field => resp = field);
        button.nativeElement.click();

        tick();

        expect(resp).toEqual(field);
    }));
});