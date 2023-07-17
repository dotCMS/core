/* eslint-disable @typescript-eslint/no-explicit-any */

import * as _ from 'lodash';
import { DragulaModule, DragulaService } from 'ng2-dragula';
import { Observable, of, Subject } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import {
    Component,
    DebugElement,
    EventEmitter,
    Injectable,
    Input,
    Output,
    Renderer2
} from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { CheckboxModule } from 'primeng/checkbox';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotDialogActions, DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, DotEventsSocket, LoginService } from '@dotcms/dotcms-js';
import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotFieldVariable
} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    cleanUpDialog,
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    dotcmsContentTypeFieldBasicMock,
    fieldsBrokenWithColumns,
    fieldsWithBreakColumn,
    FieldUtil,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { ContentTypeFieldsDropZoneComponent } from '.';

import { ContentTypeFieldsAddRowModule } from '..';
import { DotConvertToBlockInfoComponent } from '../../dot-convert-to-block-info/dot-convert-to-block-info.component';
import { DotConvertWysiwygToBlockComponent } from '../../dot-convert-wysiwyg-to-block/dot-convert-wysiwyg-to-block.component';
import { DotContentTypeFieldsVariablesModule } from '../dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { FieldPropertyService } from '../service/field-properties.service';
import { FieldService } from '../service/field.service';
import { FieldDragDropService } from '../service/index';

const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();

const fakeContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    id: '1234567890',
    name: 'ContentTypeName',
    variable: 'helloVariable',
    baseType: 'testBaseType'
};

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
    @Input()
    contentType: DotCMSContentType;

    public destroy(): void {
        return;

        return;
    }
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
    show(): void {
        return;
    }

    hide(): void {
        return;
    }
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

    beforeEach(waitForAsync(() => {
        dragDropService = new TestFieldDragDropService();

        TestBed.configureTestingModule({
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
                UiDotIconButtonModule,
                DotIconModule,
                DragulaModule,
                TableModule,
                DotFieldValidationMessageModule,
                ReactiveFormsModule,
                HttpClientTestingModule,
                DotMessagePipe,
                TabViewModule
            ],
            providers: [
                { provide: Router, useValue: mockRouter },
                { provide: FieldDragDropService, useValue: dragDropService },
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DotLoadingIndicatorService,
                    useValue: dotLoadingIndicatorServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotEventsSocket,
                LoginService,
                DotFormatDateService,
                FieldService,
                FieldPropertyService,
                DragulaService,
                DotEventsService,
                { provide: DotMessageDisplayService, useValue: {} },
                { provide: DotHttpErrorManagerService, useValue: {} }
            ]
        });

        fixture = TestBed.createComponent(ContentTypeFieldsDropZoneComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
    }));

    it('should have propertiesForm', () => {
        expect(comp.propertiesForm).not.toBeUndefined();
    });

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
        expect(dialog.attributes.width).toBe('45rem');
        expect(dialog).not.toBeNull();
    });

    it('should pass contentType', () => {
        comp.contentType = fakeContentType;
        comp.displayDialog = true;
        fixture.detectChanges();
        const contentTypeFieldsPropertyForm = de.query(
            By.css('dot-content-type-fields-properties-form')
        );
        expect(contentTypeFieldsPropertyForm.componentInstance.contentType.name).toBe(
            'ContentTypeName'
        );
    });

    it('should reset values when close dialog', async () => {
        const fieldRow: DotCMSContentTypeLayoutRow = FieldUtil.createFieldRow(1);
        comp.fieldRows = [fieldRow];

        comp.displayDialog = true;
        comp.activeTab = 1;
        spyOn(comp, 'setDialogOkButtonState');

        fixture.detectChanges();

        const dialog = de.query(By.css('dot-dialog')).componentInstance;
        dialog.hide.emit();

        await fixture.whenStable();

        expect(comp.displayDialog).toBe(false);
        expect(comp.hideButtons).toBe(false);
        expect(comp.currentField).toBe(null);
        expect(comp.activeTab).toBe(0);
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
        dotEventsService.notify('add-tab-divider', null);

        fixture.detectChanges();

        const dialog: DotDialogComponent = de.query(By.css('dot-dialog')).componentInstance;
        dialog.hide.emit();

        expect(comp.fieldRows.length).toBe(0);
    });

    afterEach(() => {
        cleanUpDialog(fixture);
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

    constructor() {
        return;
    }
}

// TODO: Upgrade tests to use FieldDragDropService (without mocking) and mocking DragulaService
// Issue ref: dotCMS/core#16772 When you DnD a field (reorder) in the same column it shows up the edit field dialog
// https://github.com/dotCMS/core-web/pull/1085

const BLOCK_EDITOR_FIELD = {
    ...dotcmsContentTypeFieldBasicMock,
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    id: '12',
    name: 'field 12',
    sortOrder: 12,
    contentTypeId: '12b'
};

@Component({
    selector: 'dot-block-editor-settings',
    template: ''
})
class TestDotBlockEditorSettingsComponent {
    @Output() changeControls = new EventEmitter<DotDialogActions>();
    @Output() valid = new EventEmitter<boolean>();
    @Output() save = new EventEmitter<DotFieldVariable[]>();

    @Input() field: DotCMSContentTypeField;
    @Input() isVisible = false;
}

describe('Load fields and drag and drop', () => {
    const dotLoadingIndicatorServiceMock: TestDotLoadingIndicatorService =
        new TestDotLoadingIndicatorService();
    let hostComp: TestHostComponent;
    let hostDe: DebugElement;
    let comp: ContentTypeFieldsDropZoneComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let scrollIntoViewSpy;

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

    beforeEach(waitForAsync(() => {
        testFieldDragDropService = new TestFieldDragDropService();

        TestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestDotContentTypeFieldsTabComponent,
                TestHostComponent,
                TestDotLoadingIndicatorComponent,
                DotConvertToBlockInfoComponent,
                DotConvertWysiwygToBlockComponent,
                TestDotBlockEditorSettingsComponent
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
                FormsModule,
                CheckboxModule,
                ReactiveFormsModule,
                BrowserAnimationsModule,
                DotActionButtonModule,
                DotIconModule,
                UiDotIconButtonModule,
                TableModule,
                ContentTypeFieldsAddRowModule,
                DotDialogModule,
                HttpClientTestingModule,
                DotMessagePipe,
                TabViewModule
            ],
            providers: [
                DragulaService,
                FieldPropertyService,
                {
                    provide: FieldService,
                    useValue: {
                        loadFieldTypes() {
                            return of([
                                {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                                    helpText:
                                        'Show a rich text area for content input that allows a user to format content.',
                                    id: 'wysiwyg',
                                    label: 'WYSIWYG',
                                    properties: [
                                        'name',
                                        'required',
                                        'regexCheck',
                                        'defaultValue',
                                        'hint',
                                        'searchable',
                                        'indexed'
                                    ]
                                },
                                {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                                    id: 'block editor',
                                    label: 'BLOCK EDITOR',
                                    properties: ['name', 'body', 'required', 'indexed']
                                }
                            ]);
                        }
                    }
                },
                DotFormatDateService,
                LoginService,
                DotEventsSocket,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: FieldDragDropService, useValue: testFieldDragDropService },
                { provide: Router, useValue: mockRouter },
                {
                    provide: DotLoadingIndicatorService,
                    useValue: dotLoadingIndicatorServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotEventsService
            ]
        });

        fixture = TestBed.createComponent(TestHostComponent);
        hostComp = fixture.componentInstance;
        hostDe = fixture.debugElement;
        de = hostDe.query(By.css('dot-content-type-fields-drop-zone'));
        comp = de.componentInstance;
        const rendered = de.injector.get(Renderer2);
        scrollIntoViewSpy = jasmine.createSpy();

        spyOn(rendered, 'selectRootElement').and.callFake(() => {
            return {
                scrollIntoView: scrollIntoViewSpy
            };
        });

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
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
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
            },
            {
                divider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    id: '10',
                    name: 'field 10',
                    sortOrder: 10,
                    contentTypeId: '10b'
                },
                columns: [
                    {
                        columnDivider: {
                            ...dotcmsContentTypeFieldBasicMock,
                            name: 'field 11',
                            id: '11',
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            sortOrder: 11,
                            contentTypeId: '11b'
                        },
                        fields: [BLOCK_EDITOR_FIELD]
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
        expect<any>(spy).toHaveBeenCalledWith(field);
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
        dotEventsService.notify('add-tab-divider', null);

        expect(comp.fieldRows.length).toBe(5);
        expect(comp.fieldRows[comp.fieldRows.length - 1].divider.clazz).toBe(
            'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
        );
    });

    it('should have FieldRow and FieldColumn', () => {
        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));

        // - 1 because one Mock Fields has not columns
        expect(fakeFields.length - 1).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });

    it('should set dropped field if a drop event happen from source', () => {
        return fixture.whenStable().then(() => {
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

            expect(comp.currentField).toBe(dropField);
        });
    });

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

        expect<any>(spy).toHaveBeenCalledWith(field);
    });

    it('should disable field variable tab', () => {
        comp.currentField = {
            ...dotcmsContentTypeFieldBasicMock
        };
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.p-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('p-disabled')).toBe(true);
    });

    it('should NOT disable field variable tab', () => {
        comp.currentField = {
            ...dotcmsContentTypeFieldBasicMock,
            id: '123'
        };
        comp.displayDialog = true;
        fixture.detectChanges();
        const tabLinks = de.queryAll(By.css('.p-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('p-disabled')).toBe(false);
    });

    it('should change the dialogActions', () => {
        const newDialogActions = {
            accept: {
                action: () => {
                    /* */
                },
                label: 'Save',
                disabled: true
            },
            cancel: {
                label: 'Cancel'
            }
        };
        comp.changesDialogActions(newDialogActions);
        expect(comp.dialogActions).toEqual(newDialogActions);
    });

    it('should restore the default dialogActions on Overview Tab', async () => {
        fixture.detectChanges();
        const newDialogActions = {
            accept: {
                action: () => {
                    /* */
                },
                label: 'Save',
                disabled: true
            },
            cancel: {
                label: 'Cancel'
            }
        };

        // Changes Dialog Actions
        comp.changesDialogActions(newDialogActions);
        expect(comp.dialogActions).toEqual(newDialogActions);

        // Change to Overview Tab
        comp.handleTabChange(0);
        expect(comp.dialogActions).toEqual(comp.defaultDialogActions);
    });

    it('should restore the default dialogActions on Overview Tab', async () => {
        fixture.detectChanges();
        const newDialogActions = {
            accept: {
                action: () => {
                    /* */
                },
                label: 'Save',
                disabled: true
            },
            cancel: {
                label: 'Cancel'
            }
        };

        // Changes Dialog Actions
        comp.changesDialogActions(newDialogActions);
        expect(comp.dialogActions).toEqual(newDialogActions);

        // Change to Overview Tab
        comp.handleTabChange(0);
        expect(comp.dialogActions).toEqual(comp.defaultDialogActions);
    });

    describe('Edit Field Dialog', () => {
        describe('WYSIWYG field', () => {
            let fieldBox;
            const field = {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                name: 'WYSIWYG',
                id: '3'
            };

            beforeEach(() => {
                fixture.detectChanges();

                fieldBox = de.query(By.css('dot-content-type-fields-row'));
                fieldBox.componentInstance.editField.emit(field);

                fixture.detectChanges();
            });
            it('should show info box and scrollTo on click', () => {
                const infoBox = de.query(By.css('dot-convert-to-block-info'));

                expect(infoBox.componentInstance.currentField.id).toBe('3');
                expect(infoBox.componentInstance.currentFieldType.id).toBe('wysiwyg');

                infoBox.triggerEventHandler('action', {});

                expect(scrollIntoViewSpy).toHaveBeenCalledWith({
                    behavior: 'smooth',
                    block: 'start',
                    inline: 'nearest'
                });
            });

            it('should show convert to block box and trigger convert', () => {
                spyOn(comp.editField, 'emit');

                const convertBox = de.query(By.css('dot-convert-wysiwyg-to-block'));

                convertBox.triggerEventHandler('convert', {});

                expect(comp.editField.emit).toHaveBeenCalledWith(
                    jasmine.objectContaining({
                        contentTypeId: '3b',
                        fieldType: 'Story-Block',
                        id: '3',
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField'
                    })
                );
            });
        });

        describe('BLOCK EDITOR field', () => {
            let fieldBoxComponent;
            let BLOCK_EDITOR_SETTINGS: DebugElement;
            let blockEditorComponent: TestDotBlockEditorSettingsComponent;

            beforeEach(() => {
                fixture.detectChanges();
                fieldBoxComponent = de.query(By.css('dot-content-type-fields-row'))
                    .componentInstance as TestContentTypeFieldsRowComponent;
                fieldBoxComponent.editField.emit(BLOCK_EDITOR_FIELD);
                fixture.detectChanges();

                BLOCK_EDITOR_SETTINGS = de.query(By.css('dot-block-editor-settings'));
                blockEditorComponent = BLOCK_EDITOR_SETTINGS.componentInstance;
            });

            it('should create dot-block-editor-settings', () => {
                const panels = de.queryAll(By.css('p-tabPanel'));
                expect(BLOCK_EDITOR_SETTINGS).toBeTruthy();
                expect(blockEditorComponent.field).toEqual(BLOCK_EDITOR_FIELD);
                expect(panels.length).toBe(3);
            });

            it('should emit changeControls and update dialogActions', () => {
                const BLOCK_EDITOR_SETTINGS = de.query(By.css('dot-block-editor-settings'));
                const blockEditorComponent =
                    BLOCK_EDITOR_SETTINGS.componentInstance as TestDotBlockEditorSettingsComponent;
                const newDialogActions = {
                    accept: {
                        action: () => {
                            /* */
                        },
                        label: 'Save',
                        disabled: true
                    },
                    cancel: {
                        label: 'Cancel'
                    }
                };
                blockEditorComponent.changeControls.emit(newDialogActions);
                fixture.detectChanges();
                expect(comp.dialogActions).toEqual(newDialogActions);
            });

            it('should close dialog on save', () => {
                blockEditorComponent.save.emit([]);
                fixture.detectChanges();
                expect(comp.displayDialog).toBe(false);
                expect(comp.dialogActions).toEqual(comp.defaultDialogActions);
                expect(comp.activeTab).toBe(comp.OVERVIEW_TAB_INDEX);
            });
        });

        it('should not create dot-block-editor-settings', () => {
            fixture.detectChanges();
            const fieldBoxComponent = de.query(By.css('dot-content-type-fields-row'))
                .componentInstance as TestContentTypeFieldsRowComponent;
            fieldBoxComponent.editField.emit({
                clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                name: 'WYSIWYG',
                id: '3'
            } as DotCMSContentTypeField);
            fixture.detectChanges();

            const BLOCK_EDITOR_SETTINGS = de.query(By.css('dot-block-editor-settings'));
            expect(BLOCK_EDITOR_SETTINGS).not.toBeTruthy();
        });

        it('should show block editor info message when create a WYSIWYG', () => {
            fixture.detectChanges();

            // Trigger create a field
            testFieldDragDropService._fieldDropFromSource.next({
                item: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField'
                }
            });

            fixture.detectChanges();

            const infoBox = de.query(By.css('dot-convert-to-block-info'));
            expect(infoBox).not.toBeNull();

            const convertBox = de.query(By.css('dot-convert-wysiwyg-to-block'));
            expect(convertBox).toBeNull();
        });

        it('should display dialog if a drop event happen from source', () => {
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

            expect(comp.displayDialog).toBe(true);
            const dialog = de.query(By.css('dot-dialog'));
            expect(dialog).not.toBeNull();
        });

        it('should set hideButtons to true when change to variable tab', () => {
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

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
