import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesEditComponent } from './content-types-edit.component';
import { CrudService } from '../../../api/services/crud/crud.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { ContentTypeField } from '../fields';
import { FieldService } from '../fields/service';
import { Location } from '@angular/common';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { ContentType } from '../shared/content-type.model';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { ContentTypesFormComponent } from '../form';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotHttpErrorManagerService } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { HotkeysService } from 'angular2-hotkeys';
import { TestHotkeysMock } from '../../../test/hotkeys-service.mock';

@Component({
    selector: 'dot-content-type-fields-drop-zone',
    template: ''
})
class TestContentTypeFieldsDropZoneComponent {
    @Input() fields: ContentTypeField[];
    @Output() saveFields = new EventEmitter<ContentTypeField[]>();
    @Output() removeFields = new EventEmitter<ContentTypeField[]>();
}

@Component({
    selector: 'dot-content-type-layout',
    template: '<ng-content></ng-content>'
})
class TestContentTypeLayoutComponent {
    @Input() contentTypeId: string;
}

@Component({
    selector: 'dot-content-types-form',
    template: ''
})
class TestContentTypesFormComponent {
    @Input() data: any;
    @Input() fields: ContentTypeField[];
    // tslint:disable-next-line:no-output-on-prefix
    @Output() submit: EventEmitter<any> = new EventEmitter();

    resetForm = jasmine.createSpy('resetForm');

    submitForm(): void {}
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.action.form.cancel': 'Cancel',
    'contenttypes.action.edit': 'Edit',
    'contenttypes.action.create': 'Create',
    'contenttypes.action.update': 'Update',
    'contenttypes.content.variable': 'Variable',
    'contenttypes.content.edit.contenttype': 'Edit {0}',
    'contenttypes.content.create.contenttype': 'Create {0}',
    'contenttypes.form.identifier': 'Idenfifier',
    'contenttypes.content.content': 'Content'
});

const getConfig = (route) => {
    return {
        declarations: [
            ContentTypesEditComponent,
            TestContentTypeFieldsDropZoneComponent,
            TestContentTypesFormComponent,
            TestContentTypeLayoutComponent
        ],
        imports: [
            RouterTestingModule.withRoutes([
                {
                    path: 'content-types-angular',
                    component: ContentTypesEditComponent
                }
            ]),
            BrowserAnimationsModule
        ],
        providers: [
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: ActivatedRoute,
                useValue: { data: Observable.of(route) }
            },
            {
                provide: HotkeysService,
                useValue: testHotKeysMock
            },
            CrudService,
            FieldService,
            ContentTypesInfoService,
            DotMenuService,
            Location
        ]
    };
};

let comp: ContentTypesEditComponent;
let fixture: ComponentFixture<ContentTypesEditComponent>;
let de: DebugElement;
let el: HTMLElement;
let crudService: CrudService;
let location: Location;
let dotRouterService: DotRouterService;
let dotHttpErrorManagerService: DotHttpErrorManagerService;
let testHotKeysMock: TestHotkeysMock;

describe('ContentTypesEditComponent create mode', () => {
    beforeEach(async(() => {
        testHotKeysMock = new TestHotkeysMock();
        const configCreateMode = getConfig({
            contentType: {
                baseType: 'CONTENT'
            }
        });

        DOTTestBed.configureTestingModule(configCreateMode);

        fixture = DOTTestBed.createComponent(ContentTypesEditComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        crudService = de.injector.get(CrudService);
        location = de.injector.get(Location);
        dotRouterService = de.injector.get(DotRouterService);
        dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);

        fixture.detectChanges();
    }));

    it('should has dialog opened by default', () => {
        const dialog = de.query(By.css('p-dialog'));
        expect(dialog).not.toBeNull();
        expect(dialog.componentInstance.visible).toBeTruthy();
    });

    it('should have cancel button', () => {
        const cancelButton = de.query(By.css('.content-type__cancel'));
        expect(cancelButton === null).toBe(false);
    });

    it('should have save button and disabled by default', () => {
        const saveButton = de.query(By.css('.content-type__save'));
        expect(saveButton === null).toBe(false);
        expect(saveButton.nativeElement.disabled).toBe(true);
    });

    it('should close the dialog and redirect', () => {
        spyOn(dotRouterService, 'gotoPortlet');
        spyOn(comp, 'cancelForm').and.callThrough();

        const cancelButton = de.query(By.css('.content-type__cancel'));
        cancelButton.nativeElement.click();
        fixture.detectChanges();

        expect(comp.cancelForm).toHaveBeenCalledTimes(1);
        expect(comp.show).toBe(false);
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
    });

    it('should close dialog and redirect on esc key', () => {
        spyOn(comp, 'cancelForm').and.callThrough();

        testHotKeysMock.callback(['esc']);
        fixture.detectChanges();

        expect(comp.cancelForm).toHaveBeenCalledTimes(1);
    });

    it('should NOT have dot-content-type-layout', () => {
        const contentTypeLayout = de.query(By.css('dot-content-type-layout'));
        expect(contentTypeLayout === null).toBe(true);
    });

    it('should have show form by default', () => {
        const dialog = de.query(By.css('p-dialog'));
        const contentTypeForm = de.query(By.css('dot-content-types-form'));
        expect(contentTypeForm === null).toBe(false);
        expect(dialog === null).toBe(false);
    });

    it('should NOT have dot-content-type-fields-drop-zone', () => {
        const contentTypeForm = de.query(By.css('dot-content-type-fields-drop-zone'));
        expect(contentTypeForm === null).toBe(true);
    });

    it('should have create title create mode', () => {
        const dialogTitle: DebugElement = de.query(By.css('p-header'));
        expect(dialogTitle).toBeTruthy();
        expect(dialogTitle.nativeElement.innerText).toEqual('Create Content');
    });

    describe('create', () => {
        let mockContentType: ContentType;
        let contentTypeForm: DebugElement;

        beforeEach(() => {
            mockContentType = {
                clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                host: null,
                name: 'Hello World',
                owner: '123',
                system: false
            };

            contentTypeForm = de.query(By.css('dot-content-types-form'));
        });

        it('should create content type', () => {
            const responseContentType = Object.assign({}, { id: '123' }, mockContentType, {
                fields: [{ hello: 'world' }]
            });

            spyOn(crudService, 'postData').and.returnValue(Observable.of([responseContentType]));
            spyOn(location, 'replaceState').and.returnValue(Observable.of([responseContentType]));

            contentTypeForm.triggerEventHandler('submit', mockContentType);

            expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockContentType);
            expect(comp.data).toEqual(responseContentType, 'set data with response');
            expect(comp.fields).toEqual(responseContentType.fields, 'ser fields with response');
            expect(location.replaceState).toHaveBeenCalledWith('/content-types-angular/edit/123');
        });

        it('should handle error', () => {
            spyOn(crudService, 'postData').and.returnValue(Observable.throw(mockResponseView(403)));
            spyOn(dotRouterService, 'gotoPortlet');
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();

            contentTypeForm.triggerEventHandler('submit', mockContentType);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        });
    });

    describe('bind submit button', () => {
        let form: ContentTypesFormComponent;

        beforeEach(() => {
            form = de.query(By.css('dot-content-types-form')).componentInstance;
            spyOn(form, 'submitForm');
            form.canSave = true;
            fixture.detectChanges();
        });

        it('should bind save button disabled attribute to canSave property from the form', () => {
            const saveButton = de.query(By.css('.content-type__save'));
            expect(saveButton === null).toBe(false);
            expect(saveButton.nativeElement.disabled).toBe(false);
        });

        it('should submit form when save button is clicked', () => {
            const saveButton = de.query(By.css('.content-type__save'));
            saveButton.nativeElement.click();
            expect(form.submitForm).toHaveBeenCalledTimes(1);
        });
    });
});

const currentFieldsInServer = [
    {
        name: 'fieldName',
        id: '4',
        clazz: 'fieldClass',
        sortOrder: 1
    },
    {
        name: 'field 3',
        id: '3',
        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
        sortOrder: 3
    }
];

const fakeContentType: ContentType = {
    baseType: 'CONTENT',
    id: '1234567890',
    clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
    fields: currentFieldsInServer,
    defaultType: true,
    fixed: true,
    folder: 'folder',
    host: 'host',
    name: 'name',
    owner: 'owner',
    system: false
};

const configEditMode = getConfig({
    contentType: fakeContentType
});

describe('ContentTypesEditComponent edit mode', () => {
    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(configEditMode);

        fixture = DOTTestBed.createComponent(ContentTypesEditComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;

        crudService = fixture.debugElement.injector.get(CrudService);
        location = fixture.debugElement.injector.get(Location);
        dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        dotHttpErrorManagerService = fixture.debugElement.injector.get(DotHttpErrorManagerService);
        fixture.detectChanges();
    }));

    const clickEditButton = () => {
        const editButton: DebugElement = fixture.debugElement.query(By.css('#form-edit-button'));
        editButton.nativeNode.click();
        fixture.detectChanges();
    };

    it('should have dot-content-type-layout', () => {
        const contentTypeLayout = de.query(By.css('dot-content-type-layout'));
        expect(contentTypeLayout === null).toBe(false);
    });

    it('should have dot-content-type-fields-drop-zone', () => {
        const contentTypeForm = de.query(By.css('dot-content-type-fields-drop-zone'));
        expect(contentTypeForm === null).toBe(false);
    });

    it('should have edit button', () => {
        const editButton: DebugElement = fixture.debugElement.query(By.css('#form-edit-button'));
        expect(editButton.nativeElement.disabled).toBe(false);
        expect(editButton).toBeTruthy();
    });

    it('should have edit content type title', () => {
        clickEditButton();

        const dialogTitle: DebugElement = de.query(By.css('p-header'));
        expect(dialogTitle).toBeTruthy();
        expect(dialogTitle.nativeElement.innerText).toEqual('Edit Content');
    });

    it('should open dialog on edit button click', () => {
        clickEditButton();

        const dialog = de.query(By.css('p-dialog'));
        expect(dialog).not.toBeNull();
        expect(comp.show).toBeTruthy();
        expect(dialog.componentInstance.visible).toBeTruthy();
    });

    it('should close the dialog', () => {
        clickEditButton();

        spyOn(dotRouterService, 'gotoPortlet');

        spyOn(comp, 'cancelForm').and.callThrough();
        const cancelButton = de.query(By.css('.content-type__cancel'));
        cancelButton.nativeElement.click();
        fixture.detectChanges();

        expect(comp.cancelForm).toHaveBeenCalledTimes(1);
        expect(comp.show).toBe(false);
        expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
    });

    it('should save fields on dropzone event', () => {
        const newFieldsAdded: ContentTypeField[] = [
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
            }
        ];

        const fieldsReturnByServer: ContentTypeField[] = newFieldsAdded.concat(currentFieldsInServer);
        const fieldService = fixture.debugElement.injector.get(FieldService);
        spyOn(fieldService, 'saveFields').and.returnValue(Observable.of(fieldsReturnByServer));

        const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

        // when: the saveFields event is tiggered in content-type-fields-drop-zone
        contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

        // then: the saveFields method has to be called in FileService ...
        expect(fieldService.saveFields).toHaveBeenCalledWith('1234567890', newFieldsAdded);
        // ...and the comp.data.fields has to be set to the fields return by the service
        expect(comp.fields).toEqual(fieldsReturnByServer);
    });

    it('should remove fields on dropzone event', () => {
        const fieldsReturnByServer: ContentTypeField[] = currentFieldsInServer.slice(-1);
        const fieldService = fixture.debugElement.injector.get(FieldService);
        spyOn(fieldService, 'deleteFields').and.returnValue(Observable.of({ fields: fieldsReturnByServer }));

        const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

        const fieldToRemove = {
            name: 'field 3',
            id: '3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            sortOrder: 3
        };

        // when: the saveFields event is tiggered in content-type-fields-drop-zone
        contentTypeFieldsDropZone.componentInstance.removeFields.emit(fieldToRemove);

        // then: the saveFields method has to be called in FileService ...
        expect(fieldService.deleteFields).toHaveBeenCalledWith('1234567890', fieldToRemove);
        // ...and the comp.data.fields has to be set to the fields return by the service
        expect(comp.fields).toEqual(fieldsReturnByServer);
    });

    describe('update', () => {
        let contentTypeForm: DebugElement;

        beforeEach(() => {
            clickEditButton();
            contentTypeForm = de.query(By.css('dot-content-types-form'));
        });

        it('should udpate content type', () => {
            const responseContentType = Object.assign({}, fakeContentType, {
                fields: [{ hello: 'world' }]
            });

            spyOn(crudService, 'putData').and.returnValue(Observable.of(responseContentType));

            contentTypeForm.triggerEventHandler('submit', fakeContentType);

            expect(crudService.putData).toHaveBeenCalledWith('v1/contenttype/id/1234567890', fakeContentType);
            expect(comp.data).toEqual(responseContentType, 'set data with response');
        });

        it('should handle error', () => {
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            spyOn(dotRouterService, 'gotoPortlet');
            spyOn(crudService, 'putData').and.returnValue(Observable.throw(mockResponseView(403)));

            contentTypeForm.triggerEventHandler('submit', fakeContentType);

            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        });
    });
});
