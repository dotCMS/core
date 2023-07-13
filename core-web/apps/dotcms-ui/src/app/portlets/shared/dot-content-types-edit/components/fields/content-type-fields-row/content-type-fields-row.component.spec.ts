import { DragulaModule, DragulaService } from 'ng2-dragula';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    dotcmsContentTypeFieldBasicMock,
    FieldUtil,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { ContentTypeFieldsRowComponent } from '.';

import { FieldDragDropService } from '../service';

const mockFieldRow: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(2);

mockFieldRow.columns[0].fields = [
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: 'text',
        name: 'field-1'
    },
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: 'image',
        name: 'field-1'
    }
];

mockFieldRow.columns[1].fields = [
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: 'text',
        name: 'field-1'
    }
];

@Component({
    selector: 'dot-content-type-field-dragabble-item',
    template: ''
})
class TestContentTypeFieldDraggableItemComponent {
    @Input()
    field: DotCMSContentTypeField;
    @Output()
    remove: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    edit: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
}

@Component({
    selector: 'dot-test-host',
    template: '<dot-content-type-fields-row [fieldRow]="data"></dot-content-type-fields-row>'
})
class DotTestHostComponent {
    data: DotCMSContentTypeLayoutRow;

    setData(data: DotCMSContentTypeLayoutRow): void {
        this.data = data;
    }
}

describe('ContentTypeFieldsRowComponent', () => {
    let hostFixture: ComponentFixture<DotTestHostComponent>;
    let hostDe: DebugElement;
    let hostComp: DotTestHostComponent;

    let comp: ContentTypeFieldsRowComponent;
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

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsRowComponent,
                TestContentTypeFieldDraggableItemComponent,
                DotTestHostComponent
            ],
            imports: [DragulaModule, UiDotIconButtonTooltipModule, DotMessagePipe],
            providers: [
                FieldDragDropService,
                DotAlertConfirmService,
                DragulaService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        hostFixture = DOTTestBed.createComponent(DotTestHostComponent);
        hostComp = hostFixture.componentInstance;
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-row'));
        comp = de.componentInstance;
        dotDialogService = de.injector.get(DotAlertConfirmService);
    }));

    describe('setting rows and columns', () => {
        beforeEach(() => {
            hostComp.setData(mockFieldRow);
            hostFixture.detectChanges();
        });

        it('should has row and columns', () => {
            const columns = de.queryAll(By.css('.row-columns__item'));
            expect(2).toEqual(columns.length);

            columns.forEach((col, index) => {
                expect('fields-bag').toEqual(col.attributes['dragula']);
                expect('target').toEqual(col.attributes['data-drag-type']);

                const draggableItems = col.queryAll(
                    By.css('dot-content-type-field-dragabble-item')
                );
                expect(mockFieldRow.columns[index].fields.length).toEqual(draggableItems.length);
            });
        });

        it('should handle edit field event', () => {
            let editField;

            const field = {
                ...dotcmsContentTypeFieldBasicMock,
                clazz: 'text',
                name: 'field-1'
            };

            const column = de.query(By.css('.row-columns__item'));
            const dragableItem = column.query(By.css('dot-content-type-field-dragabble-item'));

            comp.editField.subscribe((eventField) => (editField = eventField));
            dragableItem.componentInstance.edit.emit(field);

            expect(field).toEqual(editField);
        });

        it('should not show the remove row button', () => {
            const removeButon = de.query(By.css('.row-header__remove'));
            expect(removeButon === null).toBe(true);
        });
    });

    describe('remove', () => {
        describe('row', () => {
            beforeEach(() => {
                const mock: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
                hostComp.setData(mock);
                hostFixture.detectChanges();
                spyOn(dotDialogService, 'confirm');
            });

            it('should show 1 remove button', () => {
                const removeButon = de.queryAll(By.css('.row-header__remove'));
                expect(removeButon.length).toBe(1);
            });

            it('should emit row remove event with no confirmation dialog', () => {
                let result;
                comp.removeRow.subscribe((rowToRemove: DotCMSContentTypeLayoutRow) => {
                    result = rowToRemove;
                });

                const removeRowButon = de.query(By.css('.row-header__remove'));
                removeRowButon.nativeElement.click();

                expect(result).toEqual(comp.fieldRow);
                expect(dotDialogService.confirm).not.toHaveBeenCalled();
            });
        });

        describe('columns', () => {
            beforeEach(() => {
                const mock: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(2);
                hostComp.setData(mock);
                hostFixture.detectChanges();
            });

            it('should show 2 remove button', () => {
                const removeButon = de.queryAll(By.css('.row-header__remove'));
                expect(removeButon.length).toBe(2);
            });

            it('should emit remove field event', () => {
                comp.fieldRow.columns[0].columnDivider.id = 'test';

                let result;
                comp.removeField.subscribe((col: DotCMSContentTypeField) => {
                    result = col;
                });

                const removeRowButon = de.query(By.css('.row-header__remove'));
                removeRowButon.nativeElement.click();

                expect(result.clazz).toEqual(
                    'com.dotcms.contenttype.model.field.ImmutableColumnField'
                );
            });

            it('should remove column from local row and no emit', () => {
                let result;
                comp.removeField.subscribe((col: DotCMSContentTypeField) => {
                    result = col;
                });

                expect(comp.fieldRow.columns.length).toBe(2);

                const removeRowButon = de.query(By.css('.row-header__remove'));
                removeRowButon.nativeElement.click();

                expect(comp.fieldRow.columns.length).toBe(1);
                expect(result).toBeUndefined();
            });
        });
    });
});
