import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, Component, Input, SimpleChange } from '@angular/core';
import { ContentTypeFieldsDropZoneComponent } from './';
import { By } from '@angular/platform-browser';
import { Field, FieldRow, ContentTypeFieldsPropertiesFormComponent } from '../';
import { DragulaModule } from 'ng2-dragula';
import { FieldDragDropService } from '../service';
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

@Component({
    selector: 'content-type-fields-row',
    template: ''
})
class TestContentTypeFieldsRow {
    @Input() fieldRow: FieldRow;
}

// Needs to find the way to test the drop event
// https://github.com/valor-software/ng2-dragula/issues/758
xdescribe('ContentTypeFieldsDropZoneComponent', () => {
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

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsDropZoneComponent,
                TestContentTypeFieldsRow,
                ContentTypeFieldsPropertiesFormComponent
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
                FieldDragDropService,
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

    it('should has a fields container', () => {

        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));

        expect(fieldsContainer).not.toBeNull();

        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);
    });

    it('should has a fields container', () => {
        const fields: Field[] = [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                name: 'field 1'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                name: 'field 2'
            },
            {
                clazz: 'text',
                name: 'field 3'
            },
            {
                clazz: 'text',
                name: 'field 4'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTabDividerField',
                name: 'field 2'
            },
            {
                clazz: 'text',
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
            fields: new SimpleChange(null, fields, true),
        });

        fixture.detectChanges();

        const fieldsContainer = de.query(By.css('.content-type-fields-drop-zone__container'));

        expect(fieldsContainer).not.toBeNull();

        expect('target').toEqual(fieldsContainer.attributes['data-drag-type']);
        expect('fields-row-bag').toEqual(fieldsContainer.attributes['dragula']);

        const fieldRows = fieldsContainer.queryAll(By.css('content-type-fields-row'));
        expect(2).toEqual(fieldRows.length);

        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns.length);
        expect(2).toEqual(fieldRows[0].componentInstance.fieldRow.columns[0].fields.length);
        expect(1).toEqual(fieldRows[0].componentInstance.fieldRow.columns[1].fields.length);

        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns.length);
        expect(1).toEqual(fieldRows[1].componentInstance.fieldRow.columns[0].fields.length);
    });

    xit('should set dropped field if a drop event happen', () => {

    });

    xit('should display dialog if a drop event happen', () => {

    });
});
