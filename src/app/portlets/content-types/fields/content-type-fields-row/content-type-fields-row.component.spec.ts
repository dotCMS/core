import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { ContentTypeFieldsRowComponent } from './';
import { By } from '@angular/platform-browser';
import { FieldDragDropService  } from '../service';
import { Field, FieldRow, FieldColumn } from '../';
import { DragulaModule } from 'ng2-dragula';
import { Observable } from 'rxjs/Observable';
import { IconButtonTooltipModule } from '../../../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';

@Component({
    selector: 'content-type-field-dragabble-item',
    template: ''
})
class TestContentTypeFieldDraggableItemComponent {
    @Input() field: Field;
    @Output() remove: EventEmitter<Field> = new EventEmitter();
}

describe('ContentTypeFieldsRowComponent', () => {
    let comp: ContentTypeFieldsRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    const messageServiceMock = new MockMessageService({
        'contenttypes.dropzone.rows.empty.message': 'Add fields here',
    });

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsRowComponent,
                TestContentTypeFieldDraggableItemComponent
            ],
            imports: [
                DragulaModule,
                IconButtonTooltipModule
            ],
            providers: [
                FieldDragDropService,
                { provide: MessageService, useValue: messageServiceMock }
            ],
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsRowComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should has row and columns', () => {
        const fieldRow = new FieldRow();
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

        const columns = de.queryAll(By.css('.row-columns__item'));
        expect(2).toEqual(columns.length);

        columns.forEach((col, index) => {
            expect('fields-bag').toEqual(col.attributes['dragula']);
            expect('target').toEqual(col.attributes['data-drag-type']);

            const draggableItems = col.queryAll(By.css('content-type-field-dragabble-item'));
            expect(fieldRow.columns[index].fields.length).toEqual(draggableItems.length);
        });
    });
});
