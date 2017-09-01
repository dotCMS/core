import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, SimpleChange, Output, EventEmitter, Injectable } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import { Field, FieldRow, ContentTypeFieldsPropertiesFormComponent } from '../';
import { DragulaModule } from 'ng2-dragula';
import { ReactiveFormsModule } from '@angular/forms';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { MessageService } from '../../../../api/services/messages-service';
import { LoginService } from '../../../../api/services/login-service';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable } from 'rxjs/Observable';
import { SocketFactory } from '../../../../api/services/protocol/socket-factory';
import { FormatDateService } from '../../../../api/services/format-date-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Subject } from 'rxjs/Subject';
import { FieldDragDropService } from '../service/index';

@Component({
    selector: 'content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRow {
    @Input() fieldRow: FieldRow;
}

@Component({
    selector: 'content-type-fields-properties-form ',
    template: ''
})
class TestContentTypeFieldsPropertiesForm {
    @Output() saveField: EventEmitter<any> = new EventEmitter();
    @Input() formFieldData: Field;
}
@Injectable()
class TestFieldDragDropService {
    _fieldDropFromSource: Subject<any> = new Subject();
    _fieldDropFromTarget: Subject<any> = new Subject();

    get fieldDropFromSource$(): Observable<any> {
        return this._fieldDropFromSource.asObservable();
    }

    get fieldDropFromTarget$(): Observable<any> {
        return this._fieldDropFromTarget.asObservable();
    }
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
                TestContentTypeFieldsPropertiesForm
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
        fixture.detectChanges();
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect(fieldsContainer).not.toBeNull();
    });

    it('should has the right dragula attributes', () => {
        fixture.detectChanges();
        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));
        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should has a dialog', () => {
        fixture.detectChanges();

        const dialog = de.query(By.css('p-dialog'));

        expect(dialog).not.toBeNull();
    });

    describe('Drag and Drop', () => {
        beforeEach(async(() => {
            this.fields = [
                {
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
                },
                {
                    name: 'field 2',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField'
                },
                {
                    clazz: 'text',
                    id: 1,
                    name: 'field 3'
                },
                {
                    clazz: 'text',
                    id: 2,
                    name: 'field 4'
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    name: 'field 2'
                },
                {
                    clazz: 'text',
                    id: 3,
                    name: 'field 3'
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                    name: 'field 5'
                },
                {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                    name: 'field 6'
                },
                {
                    clazz: 'text',
                    name: 'field 7'
                }
            ];

            comp.ngOnChanges({
                fields: new SimpleChange(null, this.fields, true),
            });
        }));

        it('should has FieldRow and FieldColumn', () => {
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

        it('should save field if a drop event happen from target', fakeAsync(() => {
            spyOn(comp.saveFields, 'emit');
            comp.ngOnInit();

            this.testFieldDragDropService._fieldDropFromTarget.next(['fields-bag', null, null, {
                dataset: {
                    dragType: 'target'
                }
            }]);

            tick();
            fixture.detectChanges();

            expect(comp.saveFields.emit).toHaveBeenCalledWith(this.fields);
        }));
    });
});
