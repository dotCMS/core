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
import { DotConfirmationService } from '../../../../api/services/dot-confirmation-service';
import { ConfirmDialogModule } from 'primeng/primeng';
import { MockMessageService } from '../../../../test/message-service.mock';

@Component({
    selector: 'content-type-field-dragabble-item',
    template: ''
})
class TestContentTypeFieldDraggableItemComponent {
    @Input() field: Field;
    @Output() remove: EventEmitter<Field> = new EventEmitter();
    @Output() edit: EventEmitter<Field> = new EventEmitter();
}

describe('ContentTypeFieldsRowComponent', () => {
    let comp: ContentTypeFieldsRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    const messageServiceMock = new MockMessageService({
        'contenttypes.dropzone.rows.empty.message': 'Add fields here',
        'contenttypes.action.delete': 'Delete',
        'message.structure.delete.structure': 'Are you sure you want to delete this',
        'message.structure.delete.content': 'and all the content associated with it?',
        'message.structure.delete.notice': '(This operation can not be undone)',
        'contenttypes.action.yes': 'Yes',
        'contenttypes.action.no': 'No'
    });

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsRowComponent,
                TestContentTypeFieldDraggableItemComponent
            ],
            imports: [
                DragulaModule,
                IconButtonTooltipModule,
                ConfirmDialogModule
            ],
            providers: [
                FieldDragDropService,
                DotConfirmationService,
                { provide: MessageService, useValue: messageServiceMock }
            ],
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsRowComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    describe('setting rows and columns', () => {

        beforeEach(async(() => {
            this.fieldRow = new FieldRow();
            this.fieldRow.columns.push(new FieldColumn([
                {
                    clazz: 'text',
                    name: 'field-1'
                },
                {
                    clazz: 'image',
                    name: 'field-1'
                }
            ]));

            this.fieldRow.columns.push(new FieldColumn([
                {
                    clazz: 'text',
                    name: 'field-1'
                }
            ]));

            comp.fieldRow = this.fieldRow;
        }));

        it('should has row and columns', () => {
            fixture.detectChanges();

            const columns = de.queryAll(By.css('.row-columns__item'));
            expect(2).toEqual(columns.length);

            columns.forEach((col, index) => {
                expect('fields-bag').toEqual(col.attributes['dragula']);
                expect('target').toEqual(col.attributes['data-drag-type']);

                const draggableItems = col.queryAll(By.css('content-type-field-dragabble-item'));
                expect(this.fieldRow.columns[index].fields.length).toEqual(draggableItems.length);
            });
        });

        it('should handle edit field event', fakeAsync(() => {
            let editField;

            const field = {
                clazz: 'text',
                name: 'field-1'
            };

            fixture.detectChanges();

            const column = de.query(By.css('.row-columns__item'));
            const dragableItem = column.query(By.css('content-type-field-dragabble-item'));

            comp.editField.subscribe(eventField => editField = eventField);
            dragableItem.componentInstance.edit.emit(field);

            tick();

            expect(field).toEqual(editField);
        }));

        it('should handle remove field event', async(() => {
            let removeField;

            const field = this.fieldRow.columns[0].fields[0];
            fixture.detectChanges();

            const column = de.query(By.css('.row-columns__item'));
            const dragableItem = column.query(By.css('content-type-field-dragabble-item'));

            const dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);

            spyOn(dotConfirmationService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            comp.removeField.subscribe(eventField => {
                removeField = eventField;
            });
            dragableItem.componentInstance.remove.emit(field);

            expect(field).toEqual(removeField);
            const fieldRemoved = this.fieldRow.columns[0].fields.filter(columnField => columnField === field);
            expect(fieldRemoved).toEqual([]);
        }));
    });
});
