import { DragulaModule, DragulaService } from 'ng2-dragula';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { FieldUtil } from '@dotcms/utils';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypeFieldsRowComponent } from '.';

import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { FieldDragDropService } from '../service';

const mockFieldRow: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(2);

mockFieldRow.columns[0].fields = [
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: DotCMSClazzes.TEXT,
        name: 'field-1'
    },
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: DotCMSClazzes.IMAGE,
        name: 'field-1'
    }
];

mockFieldRow.columns[1].fields = [
    {
        ...dotcmsContentTypeFieldBasicMock,
        clazz: DotCMSClazzes.TEXT,
        name: 'field-1'
    }
];

@Component({
    selector: 'dot-content-type-field-dragabble-item',
    template: '',
    standalone: false
})
class TestContentTypeFieldDraggableItemComponent {
    @Input()
    field: DotCMSContentTypeField;
    @Input()
    isSmall = false;
    @Output()
    remove: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    edit: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
}

@Component({
    selector: 'dot-test-host',
    template: '<dot-content-type-fields-row [fieldRow]="data"></dot-content-type-fields-row>',
    standalone: false
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
            imports: [DragulaModule, TooltipModule, ButtonModule, DotMessagePipe],
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
        hostComp.data = mockFieldRow;
        hostDe = hostFixture.debugElement;
        hostFixture.detectChanges();
        de = hostDe.query(By.css('dot-content-type-fields-row'));
        comp = de.componentInstance;
        dotDialogService = hostFixture.debugElement.injector.get(DotAlertConfirmService);
    }));

    describe('setting rows and columns', () => {

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
                clazz: DotCMSClazzes.TEXT,
                name: 'field-1'
            };

            const column = de.query(By.css('.row-columns__item'));
            const dragableItem = column.query(By.css('dot-content-type-field-dragabble-item'));

            comp.editField.subscribe((eventField) => (editField = eventField));
            dragableItem.componentInstance.edit.emit(field);

            expect(field).toEqual(editField);
        });

        it('should not show the remove row button', () => {
            const removeButon = de.query(By.css('p-button[icon="pi pi-trash"]'));
            expect(removeButon === null).toBe(true);
        });
    });

    describe('remove', () => {
        describe('row', () => {
            let rowFixture: ComponentFixture<DotTestHostComponent>;
            let rowHostDe: DebugElement;
            let rowHostComp: DotTestHostComponent;
            let rowDe: DebugElement;
            let rowComp: ContentTypeFieldsRowComponent;

            beforeEach(() => {
                // Create fresh fixture with empty column
                rowFixture = DOTTestBed.createComponent(DotTestHostComponent);
                rowHostComp = rowFixture.componentInstance;
                const mock: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
                mock.columns[0].fields = [];
                rowHostComp.data = mock;
                rowHostDe = rowFixture.debugElement;
                rowFixture.detectChanges();
                rowDe = rowHostDe.query(By.css('dot-content-type-fields-row'));
                rowComp = rowDe.componentInstance;
                jest.spyOn(dotDialogService, 'confirm');
            });

            it('should show 1 remove button when column is empty', () => {
                const removeButtons = rowDe.queryAll(By.css('p-button'));
                expect(removeButtons.length).toBe(1);
            });

            it('should emit row remove event with no confirmation dialog', () => {
                let result;
                rowComp.removeRow.subscribe((rowToRemove: DotCMSContentTypeLayoutRow) => {
                    result = rowToRemove;
                });

                const removeButton = rowDe.query(By.css('p-button'));
                removeButton.nativeElement.querySelector('button').click();

                expect(result).toEqual(rowComp.fieldRow);
                expect(dotDialogService.confirm).not.toHaveBeenCalled();
            });
        });

        describe('columns', () => {
            let colFixture: ComponentFixture<DotTestHostComponent>;
            let colHostDe: DebugElement;
            let colHostComp: DotTestHostComponent;
            let colDe: DebugElement;
            let colComp: ContentTypeFieldsRowComponent;

            beforeEach(() => {
                // Create fresh fixture with 2 empty columns
                colFixture = DOTTestBed.createComponent(DotTestHostComponent);
                colHostComp = colFixture.componentInstance;
                const mock: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(2);
                mock.columns[0].fields = [];
                mock.columns[1].fields = [];
                colHostComp.data = mock;
                colHostDe = colFixture.debugElement;
                colFixture.detectChanges();
                colDe = colHostDe.query(By.css('dot-content-type-fields-row'));
                colComp = colDe.componentInstance;
            });

            it('should show 2 remove buttons when both columns are empty', () => {
                const removeButtons = colDe.queryAll(By.css('p-button'));
                expect(removeButtons.length).toBe(2);
            });

            it('should emit remove field event when column has id', () => {
                colComp.fieldRow.columns[0].columnDivider.id = 'test';

                let result;
                colComp.removeField.subscribe((col: DotCMSContentTypeField) => {
                    result = col;
                });

                const removeButton = colDe.query(By.css('p-button'));
                removeButton.nativeElement.querySelector('button').click();

                expect(result.clazz).toEqual(
                    'com.dotcms.contenttype.model.field.ImmutableColumnField'
                );
            });

            it('should remove column from local row and not emit when column has no id', () => {
                let result;
                colComp.removeField.subscribe((col: DotCMSContentTypeField) => {
                    result = col;
                });

                expect(colComp.fieldRow.columns.length).toBe(2);

                const removeButton = colDe.query(By.css('p-button'));
                removeButton.nativeElement.querySelector('button').click();

                expect(colComp.fieldRow.columns.length).toBe(1);
                expect(result).toBeUndefined();
            });
        });
    });
});
