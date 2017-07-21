import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { ContentTypeFieldsRowComponent } from './';
import { By } from '@angular/platform-browser';
import { FieldDragDropService  } from '../service';
import { Field, FieldRow, FieldColumn } from '../';
import { DragulaModule } from 'ng2-dragula';
import { Observable } from 'rxjs/Observable';

@Component({
    selector: 'content-type-field-dragabble-item',
    template: ''
})
class TestContentTypeFieldDraggableItemComponent {
    @Input() field: Field;
    @Output() remove: EventEmitter<Field> = new EventEmitter();
}

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypeFieldsRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsRowComponent,
                TestContentTypeFieldDraggableItemComponent
            ],
            imports: [
                DragulaModule
            ],
            providers: [
                FieldDragDropService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsRowComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should has row and columns', () => {
        let fieldRow = new FieldRow();
        fieldRow.columns.push(new FieldColumn([
            {
                clazz: 'text',
                name: 'field-1'
            },
            {
                clazz: 'image',
                name: 'field-1'
            }
        ]));

        fieldRow.columns.push(new FieldColumn([
            {
                clazz: 'text',
                name: 'field-1'
            }
        ]));

        comp.fieldRow = fieldRow;

        fixture.detectChanges();

        let spans = de.queryAll(By.css('span'));
        expect(2).toEqual(spans.length);

        spans.forEach((span, index) => {
            expect('fields-bag').toEqual(span.attributes['dragula']);
            expect('target').toEqual(span.attributes['data-drag-type']);

            let draggableItems = span.queryAll(By.css('content-type-field-dragabble-item'));
            expect(fieldRow.columns[index].fields.length).toEqual(draggableItems.length);
        });
    });
});