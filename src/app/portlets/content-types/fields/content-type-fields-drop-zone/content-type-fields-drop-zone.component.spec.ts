import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, SimpleChange, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import { Field, FieldRow } from '../';
import { DragulaModule } from 'ng2-dragula';
import { ReactiveFormsModule } from '@angular/forms';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { MessageService } from '../../../../api/services/messages-service';
import { LoginService, SocketFactory } from 'dotcms-js/dotcms-js';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable } from 'rxjs/Observable';
import { FormatDateService } from '../../../../api/services/format-date-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Subject } from 'rxjs/Subject';
import { FieldDragDropService } from '../service/index';
import {FieldPropertyService } from '../service/field-properties.service';
import { FieldService } from '../service/field.service';

@Component({
    selector: 'content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRow {
    @Input() fieldRow: FieldRow;
    @Output() editField: EventEmitter<Field> = new EventEmitter();
    @Output() removeField: EventEmitter<Field> = new EventEmitter();
}

@Component({
    selector: 'content-type-fields-properties-form ',
    template: ''
})
class TestContentTypeFieldsPropertiesForm {
    @Output() saveField: EventEmitter<any> = new EventEmitter();
    @Input() formFieldData: Field;
}

@Component({
    selector: 'p-overlayPanel',
    template: ''
})
class TestPOverlayPanelComponent {

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
    let el: HTMLElement;
    const mockRouter = {
        navigate: jasmine.createSpy('navigate')
    };
    const messageServiceMock = new MockMessageService({
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
                TestContentTypeFieldsRow,
                TestContentTypeFieldsPropertiesForm,
                TestPOverlayPanelComponent
            ],
            imports: [
                RouterTestingModule.withRoutes([{
                    component: ContentTypeFieldsDropZoneComponent,
                    path: 'test'
                }]),
                DragulaModule,
                FieldValidationMessageModule,
                ReactiveFormsModule,
                BrowserAnimationsModule
            ],
            providers: [
                { provide: FieldDragDropService, useValue: this.testFieldDragDropService},
                LoginService,
                SocketFactory,
                FormatDateService,
                FieldPropertyService,
                FieldService,
                { provide: MessageService, useValue: messageServiceMock },
                { provide: Router, useValue: mockRouter }
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsDropZoneComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
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

    it('should emit removeFields event', fakeAsync(() => {
        let fieldsToRemove;

        const field = {
            clazz: 'classField',
            name: 'nameField',
        };

        comp.removeFields.subscribe(removeFields => fieldsToRemove = removeFields);

        tick();

        comp.removeField(field);
        expect([field]).toEqual(fieldsToRemove);
    }));

    it('should emit removeFields event when a Row is removed', fakeAsync(() => {
        let fieldsToRemove: Field[];

        const fieldRow: FieldRow = new FieldRow();
        const field = {
            clazz: 'classField',
            name: 'nameField',
        };
        fieldRow.addFields([field]);

        comp.removeFields.subscribe(removeFields => fieldsToRemove = removeFields);

        tick();

        comp.removeFieldRow(fieldRow);

        expect([fieldRow.lineDivider, fieldRow.columns[0].tabDivider, field]).toEqual(fieldsToRemove);
    }));

    describe('Load fields and drag and drop', () => {
        beforeEach(async(() => {
            this.fields = [
                {
                    name: 'field 1',
                    id: 1,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                    sortOrder: 1
                },
                {
                    name: 'field 2',
                    id: 2,
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    sortOrder: 2
                },
                {
                    clazz: 'text',
                    id: 3,
                    name: 'field 3',
                    sortOrder: 3
                },
                {
                    clazz: 'text',
                    id: 4,
                    name: 'field 4',
                    sortOrder: 4
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    id: 5,
                    name: 'field 5',
                    sortOrder: 5
                },
                {
                    clazz: 'text',
                    id: 6,
                    name: 'field 6',
                    sortOrder: 6
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                    id: 7,
                    name: 'field 7',
                    sortOrder: 7
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    id: 8,
                    name: 'field 8',
                    sortOrder: 8
                },
                {
                    clazz: 'text',
                    id: 9,
                    name: 'field 9',
                    sortOrder: 9
                }
            ];
        }));

        it('should handler editField event', () => {
            const field = {
                clazz: 'classField',
                name: 'nameField',
            };
            const spy = spyOn(comp, 'editField');

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });

            fixture.detectChanges();

            const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
            const fieldRows = fieldsContainer.queryAll(By.css('content-type-fields-row'));
            fieldRows[0].componentInstance.editField.emit(field);

            expect(spy).toHaveBeenCalledWith(field);
        });

        it('should has FieldRow and FieldColumn', () => {

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });

            fixture.detectChanges();

            const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
            const fieldRows = fieldsContainer.queryAll(By.css('content-type-fields-row'));
            expect(2).toEqual(fieldRows.length);

            expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
            expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);
            expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[1].fields.length);

            expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
            expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
        });

        it('should set dropped field if a drop event happen from source', fakeAsync(() => {
            becomeNewField(this.fields[8]);
            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });

            comp.ngOnInit();

            this.testFieldDragDropService._fieldDropFromSource.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'source'
                }
            }]);

            tick();

            expect(this.fields[8]).toBe(comp.formData);
        }));

        it('should display dialog if a drop event happen from source', fakeAsync(() => {

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });

            comp.ngOnInit();

            this.testFieldDragDropService._fieldDropFromSource.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'source'
                }
            }]);

            tick();
            fixture.detectChanges();

            expect(true).toBe(comp.displayDialog);

            const dialog = de.query(By.css('p-dialog'));
            expect(true).toBe(dialog.componentInstance.visible);
        }));

        it('should save all the fields (moving the last line to the top)', fakeAsync(() => {
            const testFields = [...this.fields.slice(6, 9), ...this.fields.slice(0, 6)];

            comp.ngOnChanges({
                fields: new SimpleChange(null, testFields, true),
            });

            spyOn(comp.saveFields, 'emit');
            comp.ngOnInit();

            this.testFieldDragDropService._fieldRowDropFromTarget.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'target'
                }
            }]);

            tick();
            fixture.detectChanges();

            expect(comp.saveFields.emit).toHaveBeenCalledWith(testFields);
        }));

        it('should save all the fields (moving just the last field)', fakeAsync(() => {
            const testFields = [...this.fields.slice(0, 5), this.fields[8], ...this.fields.slice(5, 8)];

            comp.ngOnChanges({
                fields: new SimpleChange(null, testFields, true),
            });

            spyOn(comp.saveFields, 'emit');
            comp.ngOnInit();

            this.testFieldDragDropService._fieldDropFromTarget.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'target'
                }
            }]);

            tick();
            fixture.detectChanges();

            expect(comp.saveFields.emit).toHaveBeenCalledWith(testFields.slice(5, 9));
        }));

        it('should save all the new fields', fakeAsync(() => {
            let saveFields;

            becomeNewField(this.fields[6]);
            becomeNewField(this.fields[7]);
            becomeNewField(this.fields[8]);

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });
            comp.ngOnInit();

            // sleect the fields[8] as the current field
            this.testFieldDragDropService._fieldDropFromSource.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'source'
                }
            }]);

            tick();

            comp.saveFields.subscribe(fields => saveFields = fields);
            comp.saveFieldsHandler(this.fields[8]);

            tick();

            expect([this.fields[6], this.fields[7], this.fields[8]]).toEqual(saveFields);
        }));

        it('should save all updated fields', fakeAsync(() => {
            let saveFields;

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });
            comp.ngOnInit();
            comp.editField(this.fields[8]);

            tick();

            comp.saveFields.subscribe(fields => saveFields = fields);

            const fieldUpdated = {
                fixed: true,
                indexed: true
            };

            comp.saveFieldsHandler(fieldUpdated);

            tick();

            expect([this.fields[8]]).toEqual(saveFields);
            expect(this.fields[8].fixed).toEqual(true);
            expect(this.fields[8].indexed).toEqual(true);
        }));

        it('should handler removeField event', () => {
            const field = {
                clazz: 'classField',
                name: 'nameField',
            };

            const spy = spyOn(comp, 'removeField');

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });

            fixture.detectChanges();

            const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
            const fieldRows = fieldsContainer.queryAll(By.css('content-type-fields-row'));
            fieldRows[0].componentInstance.removeField.emit(field);

            expect(spy).toHaveBeenCalledWith(field);
        });
    });
});
