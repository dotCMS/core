import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from '.';
import { By } from '@angular/platform-browser';
import { ContentTypeFieldsAddRowModule } from '..';

import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from 'dotcms-models';
import { ReactiveFormsModule } from '@angular/forms';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoginService, DotEventsSocket } from 'dotcms-js';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable, Subject } from 'rxjs';
import { FormatDateService } from '@services/format-date-service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FieldDragDropService } from '../service/index';
import { FieldPropertyService } from '../service/field-properties.service';
import { FieldService } from '../service/field.service';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { HotkeysService } from 'angular2-hotkeys';
import { TestHotkeysMock } from '@tests/hotkeys-service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DragulaModule, DragulaService } from 'ng2-dragula';
import * as _ from 'lodash';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { TableModule } from 'primeng/table';
import { DotContentTypeFieldsVariablesModule } from '../dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { FieldUtil } from '../util/field-util';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import {
    dotcmsContentTypeFieldBasicMock,
    fieldsWithBreakColumn,
    fieldsBrokenWithColumns
} from '@tests/dot-content-types.mock';

const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();

@Component({
    selector: 'dot-content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRowComponent {
    @Input()
    fieldRow: DotCMSContentTypeLayoutRow;
    @Output()
    editField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    removeField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
}

@Component({
    selector: 'dot-content-type-fields-properties-form',
    template: ''
})
class TestContentTypeFieldsPropertiesFormComponent {
    @Output()
    saveField: EventEmitter<any> = new EventEmitter();
    @Input()
    formFieldData: DotCMSContentTypeField;

    public destroy(): void {}
}

@Component({
    selector: 'dot-content-type-fields-tab',
    template: ''
})
class TestDotContentTypeFieldsTabComponent {
    @Input()
    fieldTab: DotCMSContentTypeLayoutRow;

    @Output()
    editTab: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    removeTab: EventEmitter<DotCMSContentTypeLayoutRow> = new EventEmitter();
}

@Component({
    selector: 'dot-loading-indicator ',
    template: ''
})
class TestDotLoadingIndicatorComponent {
    @Input()
    fullscreen: boolean;
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

    isDraggedEventStarted(): boolean {
        return false;
    }
}

@Injectable()
class TestDotLoadingIndicatorService {
    show(): void {}

    hide(): void {}
}

function becomeNewField(field) {
    delete field.id;
    delete field.sortOrder;
}

describe('ContentTypeFieldsDropZoneComponent', () => {
    const dotLoadingIndicatorServiceMock = new TestDotLoadingIndicatorService();
    let comp: ContentTypeFieldsDropZoneComponent;
    let fixture: ComponentFixture<ContentTypeFieldsDropZoneComponent>;
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

    let dragDropService: TestFieldDragDropService;

    beforeEach(async(() => {
        dragDropService = new TestFieldDragDropService();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestContentTypeFieldsRowComponent,
                TestDotContentTypeFieldsTabComponent,
                TestDotLoadingIndicatorComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: ContentTypeFieldsDropZoneComponent,
                        path: 'test'
                    }
                ]),
                BrowserAnimationsModule,
                ContentTypeFieldsAddRowModule,
                DotContentTypeFieldsVariablesModule,
                DotDialogModule,
                DotActionButtonModule,
                DotIconButtonModule,
                DotIconModule,
                DragulaModule,
                TableModule,
                DotFieldValidationMessageModule,
                ReactiveFormsModule
            ],
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: HotkeysService, useClass: TestHotkeysMock },
                { provide: FieldDragDropService, useValue: dragDropService },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotLoadingIndicatorService, useValue: dotLoadingIndicatorServiceMock },
                DotEventsSocket,
                LoginService,
                FormatDateService,
                FieldService,
                FieldPropertyService,
                DragulaService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsDropZoneComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    }));

    it('should have fieldsContainer', () => {
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect(fieldsContainer).not.toBeNull();
    });

    it('should have the right dragula attributes', () => {
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should set Save button disable on load', () => {
        fixture.detectChanges();
        expect(comp.dialogActions.accept.disabled).toBeTruthy();
    });

    it('should have a dialog', () => {
        const dialog = de.query(By.css('dot-dialog'));
        expect(dialog).not.toBeNull();
    });

    it('should reset values when close dialog', () => {
        const fieldRow: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
        comp.fieldRows = [fieldRow];

        comp.displayDialog = true;
        spyOn(comp, 'setDialogOkButtonState');
        fixture.detectChanges();
        const dialog = de.query(By.css('dot-dialog')).componentInstance;
        dialog.hide.emit();
        fixture.detectChanges();
        expect(comp.displayDialog).toBe(false);
        expect(comp.hideButtons).toBe(false);
        expect(comp.currentField).toBe(null);
        expect(comp.dialogActiveTab).toBe(null);
        expect(comp.setDialogOkButtonState).toHaveBeenCalledWith(false);
    });

    it('should emit removeFields event', () => {
        let fieldsToRemove;

        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            clazz: 'classField',
            name: 'nameField'
        };

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

        comp.removeField(field);
        expect([field]).toEqual(fieldsToRemove);
    });

    it('should emit removeFields event when a Row is removed', () => {
        let fieldsToRemove: DotCMSContentTypeField[];

        const fieldRow: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            clazz: 'classField',
            name: 'nameField'
        };
        fieldRow.columns[0].fields = [field];
        fieldRow.divider.id = 'test';

        comp.fieldRows = [fieldRow];

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

        comp.removeFieldRow(fieldRow, 0);

        expect([fieldRow.divider, fieldRow.columns[0].columnDivider, field]).toEqual(
            fieldsToRemove
        );
    });

    it('should remove and empty row without lineDivider id, and not emit removeFields ', () => {
        const fieldRow1 = FieldUtil.createFieldRow(1);
        const fieldRow2 = FieldUtil.createFieldRow(1);
        fieldRow1.divider.id = 'test';
        comp.fieldRows = [fieldRow1, fieldRow2];

        spyOn(comp.removeFields, 'emit');
        comp.removeFieldRow(fieldRow2, 1);

        expect(comp.removeFields.emit).toHaveBeenCalledTimes(0);
        expect(comp.fieldRows).toEqual([fieldRow1]);
    });

    it('should cancel last drag and drop operation fields', () => {
        const fieldRow1: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            clazz: 'classField',
            name: 'nameField'
        };
        fieldRow1.columns[0].fields = [field];

        comp.layout = [fieldRow1];

        const fieldRow2 = FieldUtil.createFieldRow(1);
        comp.fieldRows = [fieldRow1, fieldRow2];

        comp.cancelLastDragAndDrop();

        expect(comp.fieldRows.length).toEqual(1);
        expect(comp.fieldRows[0].columns.length).toEqual(1);
        expect(comp.fieldRows[0].columns[0].fields).toEqual([field]);
    });

    it('should cancel last tab field drag and drop operation fields', () => {
        comp.layout = [];
        comp.fieldRows = [];

        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        fixture.detectChanges();
        dotEventsService.notify('add-tab-divider', {});

        fixture.detectChanges();

        const dialog: DotDialogComponent = de.query(By.css('dot-dialog')).componentInstance;
        dialog.hide.emit();

        expect(comp.fieldRows.length).toBe(0);
    });
});

let fakeFields: DotCMSContentTypeLayoutRow[];

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-content-type-fields-drop-zone [layout]="layout" [loading]="loading"></dot-content-type-fields-drop-zone>'
})
class TestHostComponent {
    layout: DotCMSContentTypeLayoutRow[];
    loading: boolean;

    constructor() {}
}

// TODO: Upgrade tests to use FieldDragDropService (without mocking) and mocking DragulaService
// Issue ref: dotCMS/core#16772 When you DnD a field (reorder) in the same column it shows up the edit field dialog
// https://github.com/dotCMS/core-web/pull/1085

describe('Load fields and drag and drop', () => {
    const dotLoadingIndicatorServiceMock: TestDotLoadingIndicatorService = new TestDotLoadingIndicatorService();
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

    let testFieldDragDropService: TestFieldDragDropService;

    beforeEach(async(() => {
        testFieldDragDropService = new TestFieldDragDropService();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestDotContentTypeFieldsTabComponent,
                TestHostComponent,
                TestDotLoadingIndicatorComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: ContentTypeFieldsDropZoneComponent,
                        path: 'test'
                    }
                ]),
                DragulaModule,
                DotFieldValidationMessageModule,
                DotContentTypeFieldsVariablesModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotActionButtonModule,
                DotIconModule,
                DotIconButtonModule,
                TableModule,
                ContentTypeFieldsAddRowModule,
                DotDialogModule
            ],
            providers: [
                DragulaService,
                FieldPropertyService,
                FieldService,
                FormatDateService,
                LoginService,
                DotEventsSocket,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: FieldDragDropService, useValue: testFieldDragDropService },
                { provide: HotkeysService, useClass: TestHotkeysMock },
                { provide: Router, useValue: mockRouter },
                { provide: DotLoadingIndicatorService, useValue: dotLoadingIndicatorServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(TestHostComponent);
        hostComp = fixture.componentInstance;
        hostDe = fixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-drop-zone'));
        comp = de.componentInstance;

        fakeFields = [
            {
                divider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    id: '1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 0,
                    contentTypeId: '1b'
                },
                columns: [
                    {
                        columnDivider: {
                            ...dotcmsContentTypeFieldBasicMock,
                            name: 'field 2',
                            id: '2',
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            sortOrder: 1,
                            contentTypeId: '2b'
                        },
                        fields: [
                            {
                                ...dotcmsContentTypeFieldBasicMock,
                                clazz: 'text',
                                id: '3',
                                name: 'field 3',
                                sortOrder: 2,
                                contentTypeId: '3b'
                            }
                        ]
                    },
                    {
                        columnDivider: {
                            ...dotcmsContentTypeFieldBasicMock,
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            id: '4',
                            name: 'field 4',
                            sortOrder: 3,
                            contentTypeId: '4b'
                        },
                        fields: [
                            {
                                ...dotcmsContentTypeFieldBasicMock,
                                clazz: 'text',
                                id: '5',
                                name: 'field 5',
                                sortOrder: 4,
                                contentTypeId: '5b'
                            }
                        ]
                    }
                ]
            },
            {
                divider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    id: '6',
                    name: 'field 6',
                    sortOrder: 5,
                    contentTypeId: '6b'
                }
            },
            {
                divider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    id: '7',
                    name: 'field 7',
                    sortOrder: 6,
                    contentTypeId: '7b'
                },
                columns: [
                    {
                        columnDivider: {
                            ...dotcmsContentTypeFieldBasicMock,
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            id: '8',
                            name: 'field 8',
                            sortOrder: 7,
                            contentTypeId: '8b'
                        },
                        fields: [
                            {
                                ...dotcmsContentTypeFieldBasicMock,
                                clazz: 'text',
                                id: '9',
                                name: 'field 9',
                                sortOrder: 8,
                                contentTypeId: '9b'
                            }
                        ]
                    }
                ]
            }
        ];

        hostComp.layout = fakeFields;
    }));

    it('should handler editField event', () => {
        const field = {
            clazz: 'classField',
            name: 'nameField'
        };
        const spy = spyOn(comp, 'editFieldHandler');

        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        fieldRows[0].componentInstance.editField.emit(field);
        expect(spy).toHaveBeenCalledWith(field);
    });

    it('should save all updated fields', fakeAsync(() => {
        spyOn(testFieldDragDropService, 'isDraggedEventStarted').and.returnValue(false);

        const updatedField = fakeFields[2].columns[0].fields[0];

        fixture.detectChanges();

        tick(100);
        comp.editFieldHandler(updatedField);

        spyOn(comp.editField, 'emit');

        const fieldUpdated = {
            ...dotcmsContentTypeFieldBasicMock,
            id: '1',
            fixed: true,
            indexed: true
        };

        expect(comp.currentField).toEqual(updatedField);

        comp.displayDialog = false;
        comp.saveFieldsHandler(fieldUpdated);

        expect(comp.editField.emit).toHaveBeenCalledWith(
            Object.assign({}, updatedField, fieldUpdated)
        );
    }));

    it('should not save any fields', fakeAsync(() => {
        comp.currentField = null;
        spyOn(testFieldDragDropService, 'isDraggedEventStarted').and.returnValue(true);

        const updatedField = fakeFields[2].columns[0].fields[0];

        fixture.detectChanges();

        tick(100);
        comp.editFieldHandler(updatedField);

        spyOn(comp.editField, 'emit');

        expect(comp.currentField).toBeNull();
    }));

    it('should emit and create 2 columns', () => {
        spyOn(comp, 'addRow');
        fixture.detectChanges();
        const addRowsContainer = de.query(By.css('dot-add-rows')).componentInstance;
        addRowsContainer.selectColums.emit(2);
        expect(comp.addRow).toHaveBeenCalled();
        expect(comp.fieldRows[0].columns.length).toBe(2);
    });

    it('should emit and create tab divider', () => {
        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        fixture.detectChanges();
        dotEventsService.notify('add-tab-divider', {});

        expect(comp.fieldRows.length).toBe(4);
        expect(comp.fieldRows[comp.fieldRows.length - 1].divider.clazz).toBe(
            'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
        );
    });

    it('should have FieldRow and FieldColumn', () => {
        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        expect(2).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });

    it('should set dropped field if a drop event happen from source', async(() => {
        const dropField = fakeFields[2].columns[0].fields[0];
        becomeNewField(dropField);
        fixture.detectChanges();

        testFieldDragDropService._fieldDropFromSource.next({
            item: dropField,
            target: {
                columnId: '8',
                model: [dropField]
            }
        });

        setTimeout(() => {
            expect(comp.currentField).toBe(dropField);
        }, 10);
    }));

    it('should do drag and drop without throwing error', () => {
        fixture.detectChanges();
        hostComp.loading = false;
        fixture.detectChanges();
        expect(hostComp.loading).toBe(false);
    });

    it('should save all the fields (moving the last line to the top)', (done) => {
        fixture.detectChanges();

        const fieldMoved = [
            _.cloneDeep(comp.fieldRows[1]),
            _.cloneDeep(comp.fieldRows[0]),
            _.cloneDeep(comp.fieldRows[2])
        ];

        comp.saveFields.subscribe((data) => {
            expect(data).toEqual(fieldMoved);
            expect(comp.fieldRows).toEqual(fieldMoved);
            done();
        });

        testFieldDragDropService._fieldRowDropFromTarget.next(fieldMoved);
    });

    it('should break columns and emit save', (done) => {
        fixture.detectChanges();
        comp.fieldRows = fieldsWithBreakColumn;

        comp.saveFields.subscribe((data) => {
            expect(data).toEqual(fieldsBrokenWithColumns);
            done();
        });

        testFieldDragDropService._fieldDropFromSource.next({
            item: {
                clazz: COLUMN_BREAK_FIELD.clazz
            }
        });
    });

    it('should not display Edit Dialog when drag & drop event happens', (done) => {
        fixture.detectChanges();

        const fieldMoved = [
            _.cloneDeep(comp.fieldRows[1]),
            _.cloneDeep(comp.fieldRows[0]),
            _.cloneDeep(comp.fieldRows[2])
        ];

        comp.saveFields.subscribe(() => {
            expect(comp.displayDialog).toBe(false);
            done();
        });

        testFieldDragDropService._fieldRowDropFromTarget.next(fieldMoved);
    });

    it('should save all the new fields and at the end DraggedStarted event should be false', (done) => {
        becomeNewField(fakeFields[2].divider);
        becomeNewField(fakeFields[2].columns[0].columnDivider);
        becomeNewField(fakeFields[2].columns[0].fields[0]);

        const newlyField = fakeFields[2].columns[0].fields[0];
        delete newlyField.id;

        fixture.detectChanges();

        spyOn(comp.propertiesForm, 'destroy');

        // select the fields[8] as the current field
        testFieldDragDropService._fieldDropFromSource.next({
            item: newlyField
        });

        comp.saveFields.subscribe((fields) => {
            expect(fakeFields).toEqual(fields);
            done();
        });
        comp.saveFieldsHandler(newlyField);
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

    it('should disable field variable tab', () => {
        comp.currentField = {
            ...dotcmsContentTypeFieldBasicMock
        };
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.ui-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('ui-state-disabled')).toBe(true);
    });

    it('should NOT disable field variable tab', () => {
        comp.currentField = {
            ...dotcmsContentTypeFieldBasicMock,
            id: '123'
        };
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.ui-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('ui-state-disabled')).toBe(false);
    });

    describe('Edit Field Dialog', () => {
        beforeEach(async(() => {
            fixture.detectChanges();

            const fieldToEdit: DotCMSContentTypeField = fakeFields[2].columns[0].fields[0];
            testFieldDragDropService._fieldDropFromSource.next({
                item: fieldToEdit,
                target: {
                    columnId: '8',
                    model: [fieldToEdit]
                }
            });

            fixture.detectChanges();
        }));

        it('should display dialog if a drop event happen from source', () => {
            expect(comp.displayDialog).toBe(true);
            const dialog = de.query(By.css('dot-dialog'));
            expect(dialog).not.toBeNull();
        });

        it('should set hideButtons to true when change to variable tab', () => {
            const tabView = de.query(By.css('p-tabView'));
            tabView.triggerEventHandler('onChange', { index: 1 });

            fixture.detectChanges();
            expect(de.query(By.css('dot-dialog')).componentInstance.hideButtons).toEqual(true);
        });
    });

    describe('DotLoadingIndicator', () => {
        it('should have dot-loading-indicator', () => {
            fixture.detectChanges();

            const dotLoadingIndicator = de.query(By.css('dot-loading-indicator'));
            expect(dotLoadingIndicator).not.toBeNull();
            expect(dotLoadingIndicator.componentInstance.fullscreen).toBe(true);
        });

        it('Should show dot-loading-indicator when loading is set to true', () => {
            hostComp.loading = true;
            spyOn(dotLoadingIndicatorServiceMock, 'show');
            fixture.detectChanges();

            expect(dotLoadingIndicatorServiceMock.show).toHaveBeenCalled();
        });

        it('Should hide dot-loading-indicator when loading is set to true', () => {
            hostComp.loading = false;
            spyOn(dotLoadingIndicatorServiceMock, 'hide');
            fixture.detectChanges();

            expect(dotLoadingIndicatorServiceMock.hide).toHaveBeenCalled();
        });
    });
});
