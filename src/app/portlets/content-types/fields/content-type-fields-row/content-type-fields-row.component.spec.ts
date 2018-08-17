import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { ContentTypeFieldsRowComponent } from './';
import { By } from '@angular/platform-browser';
import { FieldDragDropService } from '../service';
import { ContentTypeField, FieldRow, FieldColumn } from '../';
import { DragulaModule } from 'ng2-dragula';
import { IconButtonTooltipModule } from '../../../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotAlertConfirmService } from '../../../../api/services/dot-alert-confirm';

const mockFieldRow = new FieldRow();
mockFieldRow.columns = [
    new FieldColumn([
        {
            clazz: 'text',
            name: 'field-1'
        },
        {
            clazz: 'image',
            name: 'field-1'
        }
    ]),
    new FieldColumn([
        {
            clazz: 'text',
            name: 'field-1'
        }
    ])
];

const mockFieldRowFieldEmpty = new FieldRow();
mockFieldRowFieldEmpty.columns = [new FieldColumn([]), new FieldColumn([])];

@Component({
    selector: 'dot-content-type-field-dragabble-item',
    template: ''
})
class TestContentTypeFieldDraggableItemComponent {
    @Input() field: ContentTypeField;
    @Output() remove: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output() edit: EventEmitter<ContentTypeField> = new EventEmitter();
}

describe('ContentTypeFieldsRowComponent', () => {
    let comp: ContentTypeFieldsRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsRowComponent>;
    let de: DebugElement;
    let dotDialogService: DotAlertConfirmService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.dropzone.rows.empty.message': 'Add fields here',
        'contenttypes.action.delete': 'Delete',
        'message.structure.delete.structure': 'Are you sure you want to delete this',
        'message.structure.delete.content': 'and all the content associated with it?',
        'message.structure.delete.notice': '(This operation can not be undone)',
        'dot.common.dialog.accept': 'Yes',
        'dot.common.dialog.reject': 'No'
    });

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [ContentTypeFieldsRowComponent, TestContentTypeFieldDraggableItemComponent],
                imports: [DragulaModule, IconButtonTooltipModule],
                providers: [FieldDragDropService, DotAlertConfirmService, { provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(ContentTypeFieldsRowComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            dotDialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
        })
    );

    describe('setting rows and columns', () => {
        beforeEach(() => {
            comp.fieldRow = mockFieldRow;
        });

        it('should has row and columns', () => {
            fixture.detectChanges();

            const columns = de.queryAll(By.css('.row-columns__item'));
            expect(2).toEqual(columns.length);

            columns.forEach((col, index) => {
                expect('fields-bag').toEqual(col.attributes['dragula']);
                expect('target').toEqual(col.attributes['data-drag-type']);

                const draggableItems = col.queryAll(By.css('dot-content-type-field-dragabble-item'));
                expect(mockFieldRow.columns[index].fields.length).toEqual(draggableItems.length);
            });
        });

        it('should handle edit field event', () => {
            let editField;

            const field = {
                clazz: 'text',
                name: 'field-1'
            };

            fixture.detectChanges();

            const column = de.query(By.css('.row-columns__item'));
            const dragableItem = column.query(By.css('dot-content-type-field-dragabble-item'));

            comp.editField.subscribe((eventField) => (editField = eventField));
            dragableItem.componentInstance.edit.emit(field);

            expect(field).toEqual(editField);
        });
    });

    // Until 5.1
    xdescribe('remove rows', () => {
        it('should emit row remove event after confirmation dialog', () => {
            comp.fieldRow = mockFieldRow;
            fixture.detectChanges();

            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            let result;
            comp.removeRow.subscribe((rowToRemove: FieldRow) => {
                result = rowToRemove;
            });

            const removeRowButon = de.query(By.css('.row-header__remove'));
            removeRowButon.nativeElement.click();

            expect(comp.fieldRow).toEqual(result);
            expect(dotDialogService.confirm).toHaveBeenCalledTimes(1);
        });

        it('should emit row remove event with no confirmation dialog', () => {
            comp.fieldRow = mockFieldRowFieldEmpty;
            fixture.detectChanges();

            spyOn(dotDialogService, 'confirm');

            let result;
            comp.removeRow.subscribe((rowToRemove: FieldRow) => {
                result = rowToRemove;
            });

            const removeRowButon = de.query(By.css('.row-header__remove'));
            removeRowButon.nativeElement.click();

            expect(comp.fieldRow).toEqual(result);
            expect(dotDialogService.confirm).not.toHaveBeenCalled();
        });
    });
});
