import { async, ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import {
    DotContentTypeField,
    ContentTypeFieldsAddRowModule,
    DotFieldDivider
} from '../';
import { ReactiveFormsModule } from '@angular/forms';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotMessageService } from '@services/dot-messages-service';
import { LoginService, DotEventsSocket } from 'dotcms-js';
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
import * as _ from 'lodash';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { TableModule } from 'primeng/table';
import { DotContentTypeFieldsVariablesModule } from '../dot-content-type-fields-variables/dot-content-type-fields-variables.module';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { FieldUtil } from '../util/field-util';
import { DotEventsService } from '@services/dot-events/dot-events.service';

@Component({
    selector: 'dot-content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRowComponent {
    @Input()
    fieldRow: DotFieldDivider;
    @Output()
    editField: EventEmitter<DotContentTypeField> = new EventEmitter();
    @Output()
    removeField: EventEmitter<DotContentTypeField> = new EventEmitter();
}

@Component({
    selector: 'dot-content-type-fields-properties-form',
    template: ''
})
class TestContentTypeFieldsPropertiesFormComponent {
    @Output()
    saveField: EventEmitter<any> = new EventEmitter();
    @Input()
    formFieldData: DotContentTypeField;

    public destroy(): void {}
}

@Component({
    selector: 'dot-content-type-fields-tab',
    template: ''
})
class TestDotContentTypeFieldsTabComponent {
    @Input()
    fieldTab: DotFieldDivider;

    @Output()
    editTab: EventEmitter<DotContentTypeField> = new EventEmitter();
    @Output()
    removeTab: EventEmitter<DotFieldDivider> = new EventEmitter();
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
}

@Injectable()
class TestDotLoadingIndicatorService {
    show(): void {
    }

    hide(): void {
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

    beforeEach(async(() => {
        this.testFieldDragDropService = new TestFieldDragDropService();

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
                { provide: FieldDragDropService, useValue: this.testFieldDragDropService },
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
        const fieldRow: DotFieldDivider = FieldUtil.createFieldRow(1);
        comp.fieldRows = [fieldRow];

        comp.displayDialog = true;
        spyOn(comp, 'setDialogOkButtonState');
        fixture.detectChanges();
        const dialog = de.query(By.css('dot-dialog')).componentInstance;
        dialog.hide.emit();
        fixture.detectChanges();
        expect(comp.displayDialog).toBe(false);
        expect(comp.hideButtons).toBe(false);
        expect(comp.formData).toBe(null);
        expect(comp.dialogActiveTab).toBe(null);
        expect(comp.setDialogOkButtonState).toHaveBeenCalledWith(false);
    });

    it('should emit removeFields event', () => {
        let fieldsToRemove;

        const field = {
            clazz: 'classField',
            name: 'nameField'
        };

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));


        comp.removeField(field);
        expect([field]).toEqual(fieldsToRemove);
    });

    it('should emit removeFields event when a Row is removed', () => {
        let fieldsToRemove: DotContentTypeField[];

        const fieldRow: DotFieldDivider = FieldUtil.createFieldRow(1);
        const field = {
            clazz: 'classField',
            name: 'nameField'
        };
        fieldRow.columns[0].fields = [field];
        fieldRow.divider.id = 'test';

        comp.fieldRows = [fieldRow];

        comp.removeFields.subscribe((removeFields) => (fieldsToRemove = removeFields));

        comp.removeFieldRow(fieldRow);

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
        comp.removeFieldRow(fieldRow2);

        expect(comp.removeFields.emit).toHaveBeenCalledTimes(0);
        expect(comp.fieldRows).toEqual([fieldRow1]);
    });

    it('should cancel last drag and drop operation fields', () => {

        const fieldRow1: DotFieldDivider = FieldUtil.createFieldRow(1);
        const field = {
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
});

let fakeFields: DotFieldDivider[];

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-content-type-fields-drop-zone [layout]="layout" [loading]="loading"></dot-content-type-fields-drop-zone>'
})
class TestHostComponent {
    layout: DotFieldDivider[];
    loading: boolean;

    constructor() {}
}

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

    const moveFromSecondRowToFirstRowAndEmitEvent = () => {
        const fieldsMoved = _.cloneDeep(comp.fieldRows);
        const fieldToMove = fieldsMoved[2].columns[0].fields[0];
        fieldsMoved[2].columns[0].fields = [];
        fieldsMoved[0].columns[1].fields.unshift(fieldToMove);

        return fieldsMoved;
    };

    beforeEach(async(() => {
        this.testFieldDragDropService = new TestFieldDragDropService();

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
                { provide: FieldDragDropService, useValue: this.testFieldDragDropService },
                { provide: HotkeysService, useClass: TestHotkeysMock },
                { provide: Router, useValue: mockRouter },
                { provide: DotLoadingIndicatorService, useValue: dotLoadingIndicatorServiceMock },
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
                    name: 'field 1',
                    id: '1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 0,
                    contentTypeId: '1b'
                },
                columns: [
                    {
                        columnDivider: {
                            name: 'field 2',
                            id: '2',
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            sortOrder: 1,
                            contentTypeId: '2b'
                        },
                        fields: [
                            {
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
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            id: '4',
                            name: 'field 4',
                            sortOrder: 3,
                            contentTypeId: '4b'
                        },
                        fields: [
                            {
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
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    id: '6',
                    name: 'field 6',
                    sortOrder: 5,
                    contentTypeId: '6b'
                }
            },
            {
                divider: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    id: '7',
                    name: 'field 7',
                    sortOrder: 6,
                    contentTypeId: '7b'
                },
                columns: [
                    {
                        columnDivider:             {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            id: '8',
                            name: 'field 8',
                            sortOrder: 7,
                            contentTypeId: '8b'
                        },
                        fields: [
                            {
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
        expect(comp.fieldRows[0].columns.length).toBe(2);
    });

    it('should emit and create tab divider', () => {
        const dotEventsService: DotEventsService = de.injector.get(DotEventsService);

        fixture.detectChanges();
        dotEventsService.notify('add-tab-divider', {});

        expect(comp.fieldRows.length).toBe(4);
        expect(comp.fieldRows[comp.fieldRows.length - 1].divider.clazz)
            .toBe('com.dotcms.contenttype.model.field.ImmutableTabDividerField');
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

    it('should set dropped field if a drop event happen from source', () => {
        const dropField = fakeFields[2].columns[0].fields[0];
        becomeNewField(dropField);
        fixture.detectChanges();

        this.testFieldDragDropService._fieldDropFromSource.next({
            item: dropField,
            target: {
                columnId: '8',
                model: [dropField]
            }
        });


        expect(dropField).toBe(comp.formData);
    });

    it('should do drag and drop without throwing error', () => {
        fixture.detectChanges();
        hostComp.loading = false;
        fixture.detectChanges();
        expect(hostComp.loading).toBe(false);
    });


    it('should save all the fields (moving the last line to the top)', (done) => {
        fixture.detectChanges();

        const fieldMoved = [_.cloneDeep(comp.fieldRows[1]), _.cloneDeep(comp.fieldRows[0])];

        comp.fieldRows = [
            fakeFields[1],
            fakeFields[0],
            fakeFields[2]
        ];

        comp.saveFields.subscribe((data) => {
            const expected = [
                fakeFields[1].divider,
                fakeFields[0].divider,
                fakeFields[0].columns[0].columnDivider,
                fakeFields[0].columns[0].fields[0],
                fakeFields[0].columns[1].columnDivider,
                fakeFields[0].columns[1].fields[0],
            ].map(
                (fakeField, index) => {
                    fakeField.sortOrder = index;
                    return fakeField;
                }
            );

            expect(data).toEqual(expected);
            done();
        });

        this.testFieldDragDropService._fieldRowDropFromTarget.next(fieldMoved);
    });

    it('should save all the fields (moving just the last field)', (done) => {
        fixture.detectChanges();
        const fieldsMoved = moveFromSecondRowToFirstRowAndEmitEvent();

        comp.fieldRows = fieldsMoved;
        fixture.detectChanges();

        comp.saveFields.subscribe((data) => {
            let expectedIndex = 4;

            const expected = [
                fakeFields[2].columns[0].fields[0],
                fakeFields[0].columns[1].fields[0],
                fakeFields[1].divider,
                fakeFields[2].divider,
                fakeFields[2].columns[0].columnDivider
            ].map(
                (fakeField) => {
                    fakeField.sortOrder = expectedIndex++;
                    return fakeField;
                }
            );

            expect(data).toEqual(expected);
            done();
        });

        this.testFieldDragDropService._fieldDropFromTarget.next({});
    });

    it('should save all the new fields', (done) => {

        becomeNewField(fakeFields[2].divider);
        becomeNewField(fakeFields[2].columns[0].columnDivider);
        becomeNewField(fakeFields[2].columns[0].fields[0]);

        const newlyField = fakeFields[2].columns[0].fields[0];

        delete newlyField.id;

        fixture.detectChanges();

        spyOn(comp.propertiesForm, 'destroy');

        // select the fields[8] as the current field
        this.testFieldDragDropService._fieldDropFromSource.next({
            item: newlyField
        });

        comp.saveFields.subscribe((fields) => {
            const expected = [
                fakeFields[2].divider,
                fakeFields[2].columns[0].columnDivider,
                fakeFields[2].columns[0].fields[0]
            ];
            expected[0].sortOrder = 6;
            expected[1].sortOrder = 7;
            expected[2].sortOrder = 8;

            expect(expected).toEqual(fields);
            expect(comp.propertiesForm.destroy).toHaveBeenCalled();

            done();
        });
        comp.saveFieldsHandler(newlyField);
    });

    it('should save all updated fields', () => {
        const updatedField = fakeFields[2].columns[0].fields[0];

        fixture.detectChanges();
        comp.editField(updatedField);

        comp.saveFields.subscribe((fields) => {
            const fieldUpdated = {
                fixed: true,
                indexed: true
            };

            comp.displayDialog = false;
            comp.saveFieldsHandler(fieldUpdated);

            const { fixed, indexed, ...original } = fields[0];

            expect(original).toEqual(fakeFields[8]);
            expect(fields[0].fixed).toEqual(true);
            expect(fields[0].indexed).toEqual(true);
            expect(comp.currentField).toEqual({
                fieldId: updatedField.id,
                contentTypeId: updatedField.contentTypeId
            });
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

    it('should disable field variable tab', () => {
        comp.formData = {};
        comp.displayDialog = true;
        fixture.detectChanges();

        const tabLinks = de.queryAll(By.css('.ui-tabview-nav li'));
        expect(tabLinks[1].nativeElement.classList.contains('ui-state-disabled')).toBe(true);
    });

    it('should NOT disable field variable tab', () => {
        comp.formData = {
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

            this.testFieldDragDropService._fieldDropFromSource.next({
                item: fakeFields[7],
                target: {
                    columnId: '8',
                    model: [fakeFields[7]]
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
            tabView.triggerEventHandler('onChange', {index: 1});

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

