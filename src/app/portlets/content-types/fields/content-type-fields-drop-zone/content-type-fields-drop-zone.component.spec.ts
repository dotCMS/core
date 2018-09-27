import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import { ContentTypeField, FieldRow, ContentTypeFieldsAddRowModule } from '../';
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
import { DragulaModule, DragulaService } from 'ng2-dragula';

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
    selector: 'dot-content-type-fields-properties-form ',
    template: ''
})
class TestContentTypeFieldsPropertiesFormComponent {
    @Output()
    saveField: EventEmitter<any> = new EventEmitter();
    @Input()
    formFieldData: ContentTypeField;
}

@Component({
    // tslint:disable-next-line:component-selector
    selector: 'p-overlayPanel',
    template: ''
})
class TestPOverlayPanelComponent {}

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
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestPOverlayPanelComponent
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
                ContentTypeFieldsAddRowModule
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
        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should has a dialog', () => {
        const dialog = de.query(By.css('p-dialog'));
        expect(dialog).not.toBeNull();
    });

    it(
        'should emit removeFields event',
        fakeAsync(() => {
            let fieldsToRemove;

            const field = {
                clazz: 'classField',
                name: 'nameField'
            };

            comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

            tick();

            comp.removeField(field);
            expect([field]).toEqual(fieldsToRemove);
        })
    );

    it(
        'should emit removeFields event when a Row is removed',
        fakeAsync(() => {
            let fieldsToRemove: ContentTypeField[];

            const fieldRow: FieldRow = new FieldRow();
            const field = {
                clazz: 'classField',
                name: 'nameField'
            };
            fieldRow.addFields([field]);
            fieldRow.lineDivider.id = 'test';

            comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

            tick();

            comp.removeFieldRow(fieldRow);

            expect([fieldRow.lineDivider, fieldRow.columns[0].tabDivider, field]).toEqual(
                fieldsToRemove
            );
        })
    );

    it('should remove and empty row without lineDivider id, and not emit removeFields ', () => {
        const fieldRow1 = new FieldRow();
        const fieldRow2 = new FieldRow();
        fieldRow1.lineDivider.id = 'test';
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

    beforeEach(async(() => {
        this.testFieldDragDropService = new TestFieldDragDropService();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsRowComponent,
                TestContentTypeFieldsPropertiesFormComponent,
                TestPOverlayPanelComponent,
                TestHostComponent
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
                ContentTypeFieldsAddRowModule
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
                sortOrder: 1
            },
            {
                name: 'field 2',
                id: '2',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                sortOrder: 2
            },
            {
                clazz: 'text',
                id: '3',
                name: 'field 3',
                sortOrder: 3
            },
            {
                clazz: 'text',
                id: '4',
                name: 'field 4',
                sortOrder: 4
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                id: '5',
                name: 'field 5',
                sortOrder: 5
            },
            {
                clazz: 'text',
                id: '6',
                name: 'field 6',
                sortOrder: 6
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                id: '7',
                name: 'field 7',
                sortOrder: 7
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                id: '8',
                name: 'field 8',
                sortOrder: 8
            },
            {
                clazz: 'text',
                id: '9',
                name: 'field 9',
                sortOrder: 9
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

    it('should has FieldRow and FieldColumn', () => {
        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        const fieldRows = fieldsContainer.queryAll(By.css('dot-content-type-fields-row'));
        expect(2).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[1].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });

    it('should set dropped field if a drop event happen from source', () => {
        becomeNewField(fakeFields[8]);
        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromSource.next([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'source'
                }
            }
        ]);

        expect(fakeFields[8]).toBe(comp.formData);
    });

    it('should display dialog if a drop event happen from source', () => {
        spyOn(comp, 'addRow');
        fixture.detectChanges();
        const addRowsContainer = de.query(By.css('dot-add-rows')).componentInstance;
        addRowsContainer.selectColums.emit(2);
        expect(comp.addRow).toHaveBeenCalled();
        expect(comp.fieldRows[0].columns.length).toBe(2);
    });

    it('should display dialog if a drop event happen from source', () => {
        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromSource.next([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'source'
                }
            }
        ]);

        fixture.detectChanges();

        expect(true).toBe(comp.displayDialog);

        const dialog = de.query(By.css('p-dialog'));
        expect(true).toBe(dialog.componentInstance.visible);
    });

    it('should save all the fields (moving the last line to the top)', () => {
        const testFields = [...fakeFields.slice(6, 9), ...fakeFields.slice(0, 6)];
        hostComp.fields = testFields;

        spyOn(comp.saveFields, 'emit');

        fixture.detectChanges();

        this.testFieldDragDropService._fieldRowDropFromTarget.next([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'target'
                }
            }
        ]);

        expect(comp.saveFields.emit).toHaveBeenCalledWith(testFields);
    });

    it('should save all the fields (moving just the last field)', () => {
        const testFields = [...fakeFields.slice(0, 5), fakeFields[8], ...fakeFields.slice(5, 8)];
        hostComp.fields = testFields;

        spyOn(comp.saveFields, 'emit');

        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromTarget.next([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'target'
                }
            }
        ]);

        fixture.detectChanges();

        expect(comp.saveFields.emit).toHaveBeenCalledWith(testFields.slice(5, 9));
    });

    it('should save all the new fields', () => {
        let saveFields;

        becomeNewField(fakeFields[6]);
        becomeNewField(fakeFields[7]);
        becomeNewField(fakeFields[8]);

        fixture.detectChanges();

        // sleect the fields[8] as the current field
        this.testFieldDragDropService._fieldDropFromSource.next([
            'fields-bag',
            null,
            null,
            {
                dataset: {
                    dragType: 'source'
                }
            }
        ]);

        comp.saveFields.subscribe((fields) => (saveFields = fields));
        comp.saveFieldsHandler(fakeFields[8]);

        expect([fakeFields[6], fakeFields[7], fakeFields[8]]).toEqual(saveFields);
    });

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

        comp.saveFieldsHandler(fieldUpdated);

        const { fixed, indexed, ...original } = saveFields[0];

        expect(original).toEqual(fakeFields[8]);
        expect(saveFields[0].fixed).toEqual(true);
        expect(saveFields[0].indexed).toEqual(true);
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

        expect(comp.fieldRows[0].columns[0].fields.length).toEqual(0);
        expect(comp.fieldRows[0].columns[0].tabDivider.clazz).toEqual(
            'com.dotcms.contenttype.model.field.ImmutableColumnField'
        );
        expect(comp.fieldRows[0].lineDivider.clazz).toEqual(
            'com.dotcms.contenttype.model.field.ImmutableRowField'
        );
    });
});
