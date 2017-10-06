import { ActivatedRoute, Params, UrlSegment } from '@angular/router';
import { async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesEditComponent } from './content-types-edit.component';
import { ContentTypesFormComponent} from '../form';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { ContentTypesLayoutComponent } from '../layout';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { FieldValidationMessageModule } from '../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginService, StringUtils } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { OverlayPanelModule } from 'primeng/primeng';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { ContentType } from '../main';
import { tick, fakeAsync } from '@angular/core/testing';
import { Field } from '../fields';
import { FieldService } from '../fields/service';
import { BehaviorSubject, Subject } from 'rxjs';
import { DotConfirmationService } from '../../../api/services/dot-confirmation';

@Component({
    selector: 'content-type-fields-drop-zone',
    template: ''
})
class TestContentTypeFieldsDropZone {
    @Input() fields: Field[];

    @Output() saveFields: EventEmitter<any> = new EventEmitter();
    @Output() removeFields: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'content-type-layout',
    template: '<ng-content></ng-content>'
})
class TestContentTypeLayout {
    @Input() contentTypeId: string;
}

@Component({
    selector: 'content-types-form',
    template: ''
})
class TestContentTypesForm {
    @Input() data: any;
    @Input() icon: string;
    @Input() name: string;
    @Input() type: string;
    @Output() onCancel: EventEmitter<any> = new EventEmitter();
    @Output() onSubmit: EventEmitter<any> = new EventEmitter();

    public resetForm(): void {}
}

describe('ContentTypesEditComponent', () => {
    let comp: ContentTypesEditComponent;
    let fixture: ComponentFixture<ContentTypesEditComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let route: ActivatedRoute;
    let url: UrlSegment[];

    beforeEach(async(() => {
        const messageServiceMock = new MockMessageService({
            'message.structure.cantdelete': 'Delete Content Type',
            'message.structure.delete.structure.and.content': 'Are you sure you want to delete this Content Type?',
            'contenttypes.action.yes': 'Yes',
            'contenttypes.action.no': 'No'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypesEditComponent,
                TestContentTypesForm,
                TestContentTypeLayout,
                TestContentTypeFieldsDropZone
            ],
            imports: [
                RouterTestingModule.withRoutes([{
                    component: ContentTypesEditComponent,
                    path: 'test'
                }])
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: MessageService, useValue: messageServiceMock },
                { provide: ActivatedRoute, useValue: {'params': Observable.from([{ id: '1234' }])} },
                CrudService,
                ContentTypesInfoService,
                FieldService,
                StringUtils,
                DotConfirmationService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesEditComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
        route = fixture.debugElement.injector.get(ActivatedRoute);

        url = [
            new UrlSegment('edit', { name: 'edit' }),
            new UrlSegment('1234-identifier', { name: '1234-identifier' })
        ];

        route.url = Observable.of(url);

        const crudService = fixture.debugElement.injector.get(CrudService);

        this.getDataByIdSubjectSubject = new Subject();
        spyOn(crudService, 'getDataById').and.returnValue(this.getDataByIdSubjectSubject.asObservable());
    }));

    it('should have Content Types Layout', () => {
        const contentTypeLayout = de.query(By.css('content-type-layout'));
        const contentTypeLayoutComponentInstance = contentTypeLayout.componentInstance;
        expect(contentTypeLayout).not.toBeNull();
    });

    it('should have Content Types Form', () => {
        const contentTypeLayout = de.query(By.css('content-type-layout'));
        const contentTypeForm = contentTypeLayout.query(By.css('content-types-form'));
        expect(contentTypeForm).not.toBeNull();
    });

    it('should get the content type data with the id in the url', () => {
        const crudService = fixture.debugElement.injector.get(CrudService);

        fixture.detectChanges();

        expect(crudService.getDataById).toHaveBeenCalledWith('v1/contenttype', '1234-identifier');
    });

    it('should have call content types endpoint with widget data', () => {
        const crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'putData').and.returnValue(Observable.of({}));

        fixture.detectChanges();

        this.getDataByIdSubjectSubject.next({
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [],
            id: '1234-identifier',
        });

        comp.handleFormSubmit({
            originalEvent: Event,
            value: {
                host: '12345',
                name: 'Hello World',
            }
        });

        expect(crudService.putData).toHaveBeenCalledWith('v1/contenttype/id/1234-identifier', {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [],
            host: '12345',
            id: '1234-identifier',
            name: 'Hello World'
        });
    });

    it('should handle saveFields event', fakeAsync(() => {

        const fields = [
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
            }
        ];

        const fieldsReturnByServer: Field[] = [...fields.map((field, index) => Object.assign({id: String(index)}, field))];
        const fieldService = fixture.debugElement.injector.get(FieldService);
        const saveFieldsSpy = spyOn(fieldService, 'saveFields').and.returnValue(Observable.of(fieldsReturnByServer));

        // given: a data set...
        comp.data = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [
                {
                    name: 'fieldName',
                    clazz: 'fieldClass'
                }
            ],
            defaultType: true,
            fixed: true,
            folder: 'folder',
            host: 'host',
            name: 'name',
            owner: 'owner',
            system: false,
        };

        fixture.detectChanges();

        const contentTypeLayout = de.query(By.css('content-type-layout'));
        const contentTypeFieldsDropZone = contentTypeLayout.query(By.css('content-type-fields-drop-zone'));

        // when: the saveFields event is tiggered in content-type-fields-drop-zone
        contentTypeFieldsDropZone.componentInstance.saveFields.emit(fields);

        tick();
        // then: the saveFields method has to be called in FileService ...
        expect(saveFieldsSpy).toHaveBeenCalledWith(comp.data.id, fields);
        // ...and the comp.data.fields has to be set to the fields return by the service
        expect(comp.data.fields).toEqual(fieldsReturnByServer);
    }));

    it('should handle removeFields event', () => {
        const fields = [
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
            }
        ];

        const fieldsReturnByServer: Field[] = [
            {
                name: 'field 3',
                id: '3',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableLineDividerField',
                sortOrder: 1
            },
       ];

        const fieldService = fixture.debugElement.injector.get(FieldService);
        const deleteFieldsSpy = spyOn(fieldService, 'deleteFields').and.returnValue(Observable.of(
            {
            fields: fieldsReturnByServer,
            deletedIds: [1, 2]
            }
        ));

        // given: a data set...
        comp.data = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
            fields: [
            ],
            defaultType: true,
            fixed: true,
            folder: 'folder',
            host: 'host',
            name: 'name',
            owner: 'owner',
            system: false,
        };

        fixture.detectChanges();

        const contentTypeLayout = de.query(By.css('content-type-layout'));
        const contentTypeFieldsDropZone = contentTypeLayout.query(By.css('content-type-fields-drop-zone'));

        // when: the deleteFields event is tiggered in content-type-fields-drop-zone
        contentTypeFieldsDropZone.componentInstance.removeFields.emit(fields);

        // then: the saveFields method has to be called in FileService ...
        expect(deleteFieldsSpy).toHaveBeenCalledWith(comp.data.id, fields);
        // ...and the comp.data.fields has to be set to the fields return by the service
        expect(comp.data.fields).toEqual(fieldsReturnByServer);
    });
});

