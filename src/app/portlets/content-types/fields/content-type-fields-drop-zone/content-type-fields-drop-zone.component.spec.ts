import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import {
    ContentTypeField,
    FieldRow,
    ContentTypeFieldsAddRowModule,
    FieldTab,
    FieldDivider,
    ContentTypeFieldsVariablesComponent
} from '../';
import { ReactiveFormsModule } from '@angular/forms';
import { FieldValidationMessageModule } from '@components/_common/field-validation-message/file-validation-message.module';
import { DotMessageService } from '@services/dot-messages-service';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable, Subject } from 'rxjs';
import { FormatDateService } from '@services/format-date-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FieldDragDropService } from '../service/index';
import { FieldPropertyService } from '../service/field-properties.service';
import { FieldService } from '../service/field.service';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { HotkeysService } from 'angular2-hotkeys';
import { TestHotkeysMock } from '../../../../test/hotkeys-service.mock';
import { AddVariableFormComponent } from '../content-type-fields-variables/add-variable-form';
import { DragulaModule, DragulaService } from 'ng2-dragula';
import * as _ from 'lodash';
import { DotDialogAction } from '@components/dot-dialog/dot-dialog.component';
@Component({
    selector: 'dot-content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRowComponent {
    @Input()
    fieldRow: FieldRow;
    @Output()
    editField: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output()
    removeField: EventEmitter<ContentTypeField> = new EventEmitter();
}

@Component({
    selector: 'dot-content-type-fields-properties-form',
    template: ''
})
class TestContentTypeFieldsPropertiesFormComponent {
    @Output()
    saveField: EventEmitter<any> = new EventEmitter();
    @Input()
    formFieldData: ContentTypeField;

    public destroy(): void {}
}

@Component({
    selector: 'dot-dialog',
    template: ''
})
class DotDialogComponent {
    @Input()
    header = '';

    @Input()
    show: boolean;

    @Input()
    ok: DotDialogAction;

    @Input()
    cancel: DotDialogAction;

    @Output()
    close: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-content-type-fields-tab',
    template: ''
})
class TestDotContentTypeFieldsTabComponent {
    @Input()
    fieldTab: FieldTab;

    @Output()
    editTab: EventEmitter<ContentTypeField> = new EventEmitter();
    @Output()
    removeTab: EventEmitter<FieldDivider> = new EventEmitter();
}

@Injectable()
class TestFieldDragDropService {
    _fieldDropFromSource: Subject<any> = new Subject();
    _fieldDropFromTarget: Subject<any> = new Subject();
    _fieldRowDropFromTarget: Subject<any> = new Subject();

    get fieldDropFromSource$(): Observable<any> {
        return this._fieldDropFromSource.asObservable();
    }

    get fieldDropFromTarget$(): Observable<any> {
        return this._fieldDropFromTarget.asObservable();
    }

    get fieldRowDropFromTarget$(): Observable<any> {
        return this._fieldRowDropFromTarget.asObservable();
    }
}

function becomeNewField(field) {
    delete field.id;
}

describe('ContentTypeFieldsDropZoneComponent', () => {
    let comp: ContentTypeFieldsDropZoneComponent;
    let fixture: ComponentFixture<ContentTypeFieldsDropZoneComponent>;
    let de: DebugElement;
    let testHotKeysMock: TestHotkeysMock;
    const mockRouter = {
        navigate: jasmine.createSpy('navigate')
    };
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.dropzone.action.save': 'Save',
        'contenttypes.dropzone.action.cancel': 'Cancel',
        'contenttypes.dropzone.action.edit': 'Edit',
        'contenttypes.dropzone.action.create.field': 'Create field'
    });

    beforeEach(async(() => {
        this.testFieldDragDropService = new TestFieldDragDropService();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                ContentTypeFieldsVariablesComponent,
                AddVariableFormComponent,
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestDotContentTypeFieldsTabComponent,
                DotDialogComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: ContentTypeFieldsDropZoneComponent,
                        path: 'test'
                    }
                ]),
                DragulaModule,
                FieldValidationMessageModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotIconModule,
                DotIconButtonModule,
                ContentTypeFieldsAddRowModule,
            ],
            providers: [
                DragulaService,
                FieldPropertyService,
                FieldService,
                FormatDateService,
                LoginService,
                SocketFactory,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: FieldDragDropService, useValue: this.testFieldDragDropService },
                { provide: HotkeysService, useValue: testHotKeysMock },
                { provide: Router, useValue: mockRouter }
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsDropZoneComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        testHotKeysMock = new TestHotkeysMock();
    }));

    it('should has fieldsContainer', () => {
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect(fieldsContainer).not.toBeNull();
    });

    it('should has the right dragula attributes', () => {
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should has a dialog', () => {
        const dialog = de.query(By.css('p-dialog'));
        expect(dialog).toBeNull();
    });

    it('should reset values when close dialog', () => {
        comp.displayDialog = true;
        fixture.detectChanges();
        const dialog = de.query(By.css('dot-dialog')).componentInstance;
        dialog.close.emit();
        expect(comp.displayDialog).toBe(false);
        expect(comp.formData).toBe(null);
        expect(comp.dialogActiveTab).toBe(null);
    });

    it('should emit removeFields event', fakeAsync(() => {
        let fieldsToRemove;

        const field = {
            clazz: 'classField',
            name: 'nameField'
        };

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

        tick();

        comp.removeField(field);
        expect([field]).toEqual(fieldsToRemove);
    }));

    it('should emit removeFields event when a Row is removed', fakeAsync(() => {
        let fieldsToRemove: ContentTypeField[];

        const fieldRow: FieldRow = new FieldRow();
        const field = {
            clazz: 'classField',
            name: 'nameField'
        };
        fieldRow.addFields([field]);
        fieldRow.getFieldDivider().id = 'test';

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

        comp.removeFieldRow(fieldRow);

        tick();

        expect([fieldRow.getFieldDivider(), fieldRow.columns[0].columnDivider, field]).toEqual(
            fieldsToRemove
        );
    }));

    it('should remove and empty row without lineDivider id, and not emit removeFields ', () => {
        const fieldRow1 = new FieldRow();
        const fieldRow2 = new FieldRow();
        fieldRow1.getFieldDivider().id = 'test';
        comp.fieldRows = [fieldRow1, fieldRow2];
        spyOn(comp.removeFields, 'emit');
        comp.removeFieldRow(fieldRow2);

        expect(comp.removeFields.emit).toHaveBeenCalledTimes(0);
        expect(comp.fieldRows).toEqual([fieldRow1]);
    });
});

let fakeFields: ContentTypeField[];

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-content-type-fields-drop-zone [fields]="fields"></dot-content-type-fields-drop-zone>'
})
class TestHostComponent {
    fields: ContentTypeField[];

    constructor() {}
}

const removeSortOrder = (fieldRows: FieldRow[]) => {
    return fieldRows.map((fieldRow: FieldRow) => {
        fieldRow.getFieldDivider().sortOrder = null;
        if (fieldRow.columns) {
            fieldRow.columns = fieldRow.columns.map((column) => {
                column.columnDivider.sortOrder = null;
                column.fields = column.fields.map((field) => {
                    field.sortOrder = null;
                    return field;
                });
                return column;
            });
        }
    });
};

describe('ContentTypeFieldsDropZoneComponent', () => {
    let hostComp: TestHostComponent;
    let hostDe: DebugElement;
    let comp: ContentTypeFieldsDropZoneComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    const mockRouter = {
        navigate: jasmine.createSpy('navigate')
    };
    const messageServiceMock = new MockDotMessageService({
        'contenttypes.dropzone.action.save': 'Save',
        'contenttypes.dropzone.action.cancel': 'Cancel',
        'contenttypes.dropzone.action.edit': 'Edit',
        'contenttypes.dropzone.action.create.field': 'Create field'
    });

    const moveFromSecondRowToFirstRowAndEmitEvent = () => {
        const fieldsMoved = _.cloneDeep(comp.fieldRows);
        const fieldToMove = fieldsMoved[2].columns[0].fields[0];
        fieldsMoved[2].columns[0].fields = [];
        fieldsMoved[0].columns[1].fields.unshift(fieldToMove);

        this.testFieldDragDropService._fieldDropFromTarget.next({
            item: fieldToMove,
            source: {
                columnId: fieldsMoved[2].columns[0].columnDivider.id,
                model: fieldsMoved[2].columns[0].fields
            },
            target: {
                columnId: fieldsMoved[0].columns[1].columnDivider.id,
                model: fieldsMoved[0].columns[1].fields
            }
        });

        return fieldsMoved;
    };

    beforeEach(async(() => {
        this.testFieldDragDropService = new TestFieldDragDropService();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                ContentTypeFieldsVariablesComponent,
                AddVariableFormComponent,
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestDotContentTypeFieldsTabComponent,
                TestHostComponent,
                DotDialogComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: ContentTypeFieldsDropZoneComponent,
                        path: 'test'
                    }
                ]),
                DragulaModule,
                FieldValidationMessageModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotIconModule,
                DotIconButtonModule,
                ContentTypeFieldsAddRowModule,
            ],
            providers: [
                DragulaService,
                FieldPropertyService,
                FieldService,
                FormatDateService,
                LoginService,
                SocketFactory,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: FieldDragDropService, useValue: this.testFieldDragDropService },
                { provide: HotkeysService, useValue: new TestHotkeysMock() },
                { provide: Router, useValue: mockRouter }
            ]
        });

        fixture = DOTTestBed.createComponent(TestHostComponent);
        hostComp = fixture.componentInstance;
        hostDe = fixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-drop-zone'));
        comp = de.componentInstance;

        fakeFields = [
            {
                name: 'field 1',
                id: '1',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                sortOrder: 1,
                contentTypeId: '1b'
            },
            {
                name: 'field 2',
                id: '2',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                sortOrder: 2,
                contentTypeId: '2b'
            },
            {
                clazz: 'text',
                id: '3',
                name: 'field 3',
                sortOrder: 3,
                contentTypeId: '3b'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                id: '4',
                name: 'field 4',
                sortOrder: 4,
                contentTypeId: '4b'
            },
            {
                clazz: 'text',
                id: '5',
                name: 'field 5',
                sortOrder: 5,
                contentTypeId: '5b'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                id: '6',
                name: 'field 6',
                sortOrder: 6,
                contentTypeId: '6b'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                id: '7',
                name: 'field 7',
                sortOrder: 7,
                contentTypeId: '7b'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                id: '8',
                name: 'field 8',
                sortOrder: 8,
                contentTypeId: '8b'
            },
            {
                clazz: 'text',
                id: '9',
                name: 'field 9',
                sortOrder: 9,
                contentTypeId: '9b'
            }
        ];

        hostComp.fields = fakeFields;
    }));

    it('should handler editField event', () => {
        const field = {
            clazz: 'classField',
            name: 'nameField'
        };
        const spy = spyOn(comp, 'editField');

        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        fieldRows[0].componentInstance.editField.emit(field);
        expect(spy).toHaveBeenCalledWith(field);
    });

    it('should emit and create 2 columns', () => {
        spyOn(comp, 'addRow');
        fixture.detectChanges();
        const addRowsContainer = de.query(By.css('dot-add-rows')).componentInstance;
        addRowsContainer.selectColums.emit(2);
        expect(comp.addRow).toHaveBeenCalled();
        expect((<FieldRow>comp.fieldRows[0]).columns.length).toBe(2);
    });

    it('should has FieldRow and FieldColumn', () => {
        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        expect(2).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });

    it('should set dropped field if a drop event happen from source', fakeAsync(() => {
        becomeNewField(fakeFields[8]);
        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromSource.next({
            item: fakeFields[8],
            target: {
                columnId: '8',
                model: [fakeFields[8]]
            }
        });

        tick();

        expect(fakeFields[8]).toBe(comp.formData);
    }));

    it('should display dialog if a drop event happen from source', fakeAsync(() => {
        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromSource.next({
            item: fakeFields[7],
            target: {
                columnId: '8',
                model: [fakeFields[7]]
            }
        });

        fixture.detectChanges();

        tick();
        expect(true).toBe(comp.displayDialog);
        const dialog = de.query(By.css('dot-dialog'));
        expect(true).toBe(dialog.componentInstance.show);
    }));

    it('should save all the fields (moving the last line to the top)', fakeAsync(() => {
        spyOn(comp.saveFields, 'emit');

        fixture.detectChanges();

        const fieldMoved = [_.cloneDeep(comp.fieldRows[1]), _.cloneDeep(comp.fieldRows[0])];

        this.testFieldDragDropService._fieldRowDropFromTarget.next(fieldMoved);

        const expected = [fakeFields[5], fakeFields[0], fakeFields[1], fakeFields[2], fakeFields[3], fakeFields[4]].map(
            (fakeField, index) => {
                fakeField.sortOrder = index + 1;
                return fakeField;
            }
        );
        tick();
        expect(comp.saveFields.emit).toHaveBeenCalledWith(expected);
        expect(removeSortOrder(<FieldRow[]> comp.fieldRows)).toEqual(removeSortOrder(fieldMoved));
    }));

    it('should save all the fields (moving just the last field)', () => {
        spyOn(comp.saveFields, 'emit');

        fixture.detectChanges();
        const fieldsMoved = moveFromSecondRowToFirstRowAndEmitEvent();

        fixture.detectChanges();

        let expectedIndex = 5;

        const expected = [fakeFields[8], fakeFields[4], fakeFields[5], fakeFields[6], fakeFields[7]].map(
            (fakeField) => {
                fakeField.sortOrder = expectedIndex++;
                return fakeField;
            }
        );

        expect(comp.saveFields.emit).toHaveBeenCalledWith(expected);
        expect(removeSortOrder(<FieldRow[]>  comp.fieldRows)).toEqual(removeSortOrder(fieldsMoved));
    });

    it('should save all the new fields', fakeAsync(() => {

        let saveFields;

        becomeNewField(fakeFields[6]);
        becomeNewField(fakeFields[7]);
        becomeNewField(fakeFields[8]);

        fakeFields[7].id = 'ng-1';
        fixture.detectChanges();

        spyOn(comp.propertiesForm, 'destroy');

        // select the fields[8] as the current field
        this.testFieldDragDropService._fieldDropFromSource.next({
            item: fakeFields[8],
            target: {
                columnId: fakeFields[7].id ,
                model: [fakeFields[8]]
            }
        });
        tick();

        comp.saveFields.subscribe((fields) => (saveFields = fields));
        comp.saveFieldsHandler(fakeFields[8]);

        expect([fakeFields[6], fakeFields[7], fakeFields[8]]).toEqual(saveFields);
        expect(comp.propertiesForm.destroy).toHaveBeenCalled();
    }));

    it('should save all updated fields', () => {
        let saveFields;

        fixture.detectChanges();
        comp.editField(fakeFields[8]);

        comp.saveFields.subscribe((fields) => {
            saveFields = fields;
        });

        const fieldUpdated = {
            fixed: true,
            indexed: true
        };

        comp.displayDialog = false;
        comp.saveFieldsHandler(fieldUpdated);

        const { fixed, indexed, ...original } = saveFields[0];

        expect(original).toEqual(fakeFields[8]);
        expect(saveFields[0].fixed).toEqual(true);
        expect(saveFields[0].indexed).toEqual(true);
        expect(comp.currentField).toEqual({
            fieldId: fakeFields[8].id,
            contentTypeId: fakeFields[8].contentTypeId
        });
    });

    it('should handler removeField event', () => {
        const field = {
            clazz: 'classField',
            name: 'nameField'
        };

        const spy = spyOn(comp, 'removeField');

        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        fieldRows[0].componentInstance.removeField.emit(field);

        expect(spy).toHaveBeenCalledWith(field);
    });

    it('should create empty row and column when no fields present', () => {
        hostComp.fields = [];
        fixture.detectChanges();

        expect((<FieldRow>comp.fieldRows[0]).columns[0].fields.length).toEqual(0);
        expect((<FieldRow>comp.fieldRows[0]).columns[0].columnDivider.clazz).toEqual(
            'com.dotcms.contenttype.model.field.ImmutableColumnField'
        );
        expect(comp.fieldRows[0].getFieldDivider().clazz).toEqual(
            'com.dotcms.contenttype.model.field.ImmutableRowField'
        );
    });

    xit('it should disable field variable tab', () => {
        comp.formData = {};
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.ui-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('ui-state-disabled')).toBe(true);
    });

    xit('it should NOT disable field variable tab', () => {
        comp.formData = {
            id: '123'
        };
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.ui-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('ui-state-disabled')).toBe(false);
    });

    it('should add FieldRow when them does not exists into a TabDivider', () => {
        hostComp.fields = [
            {
                clazz: 'text',
                id: '1',
                name: 'field 1',
                sortOrder: 1,
                contentTypeId: '5b'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                id: '2',
                name: 'field 2',
                sortOrder: 2,
                contentTypeId: '6b'
            },
            {
                clazz: 'text',
                id: '3',
                name: 'field 3',
                sortOrder: 3,
                contentTypeId: '5b'
            },
        ];

        fixture.detectChanges();

        expect(comp.fieldRows.length).toEqual(3);
        expect(comp.fieldRows[0] instanceof FieldRow).toBeTruthy();
        expect(comp.fieldRows[1] instanceof FieldTab).toBeTruthy();
        expect(comp.fieldRows[2] instanceof FieldRow).toBeTruthy();
    });
});
