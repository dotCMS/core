import { throwError as observableThrowError, of as observableOf } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { ContentTypesEditComponent } from './content-types-edit.component';
import { CrudService } from '@services/crud/crud.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { ContentTypeField } from '../fields';
import { FieldService } from '../fields/service';
import { Location } from '@angular/common';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ContentTypesInfoService } from '@services/content-types-info';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotMenuService } from '@services/dot-menu.service';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HotkeysService } from 'angular2-hotkeys';
import { TestHotkeysMock } from '../../../test/hotkeys-service.mock';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { ContentType } from '@portlets/content-types/shared/content-type.model';
import { MenuItem } from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@Component({
    selector: 'dot-content-type-fields-drop-zone',
    template: ''
})
class TestContentTypeFieldsDropZoneComponent {
    @Input()
    fields: ContentTypeField[];
    @Output()
    saveFields = new EventEmitter<ContentTypeField[]>();
    @Output()
    removeFields = new EventEmitter<ContentTypeField[]>();
}

@Component({
    selector: 'dot-content-type-layout',
    template: '<ng-content></ng-content>'
})
class TestContentTypeLayoutComponent {
    @Input()
    contentTypeId: string;
}

@Component({
    selector: 'dot-content-types-form',
    template: ''
})
class TestContentTypesFormComponent {
    @Input()
    data: any;
    @Input()
    fields: ContentTypeField[];
    // tslint:disable-next-line:no-output-on-prefix
    @Output()
    onSubmit: EventEmitter<any> = new EventEmitter();

    resetForm = jasmine.createSpy('resetForm');

    submitForm(): void {}
}

@Component({
    selector: 'dot-menu',
    template: ''
})
export class TestDotMenuComponent {
    @Input()
    icon: string;
    @Input()
    float: boolean;
    @Input()
    model: MenuItem[];
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
    'contenttypes.content.content': 'Content',
    'contenttypes.dropzone.rows.add': 'Add rows',
    'contenttypes.dropzone.rows.tab_divider': 'Add tab'
});

const getConfig = (route) => {
    return {
        declarations: [
            ContentTypesEditComponent,
            TestContentTypeFieldsDropZoneComponent,
            TestContentTypesFormComponent,
            TestContentTypeLayoutComponent,
            TestDotMenuComponent
        ],
        imports: [
            RouterTestingModule.withRoutes([
                {
                    path: 'content-types-angular',
                    component: ContentTypesEditComponent
                }
            ]),
            BrowserAnimationsModule,
            DotIconModule,
            DotIconButtonModule,
            DotDialogModule
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
                useValue: { data: observableOf(route) }
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
let crudService: CrudService;
let location: Location;
let dotRouterService: DotRouterService;
let dotHttpErrorManagerService: DotHttpErrorManagerService;
let testHotKeysMock: TestHotkeysMock;
let dialog: DebugElement;

describe('ContentTypesEditComponent', () => {
    describe('create mode', () => {
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

            crudService = de.injector.get(CrudService);
            location = de.injector.get(Location);
            dotRouterService = de.injector.get(DotRouterService);
            dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);

            fixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));

            spyOn(comp, 'onDialogHide').and.callThrough();
            spyOn(dotRouterService, 'gotoPortlet');
        }));

        it('should have dialog opened by default & has css base-type class', () => {
            expect(dialog).not.toBeNull();
            expect(dialog.componentInstance.visible).toBeTruthy();
        });

        it('should set dialog actions set correctly', () => {
            expect(dialog.componentInstance.actions).toEqual({
                accept: {
                    disabled: true,
                    label: 'Create',
                    action: jasmine.any(Function)
                },
                cancel: {
                    label: 'Cancel'
                }
            });
        });

        it('should close the dialog and redirect', () => {
            const dialogCancelButton = dialog.query(By.css('.dialog__button-cancel')).nativeElement;
            dialogCancelButton.click();
            dialog.triggerEventHandler('hide', {});
            fixture.detectChanges();

            expect(comp.onDialogHide).toHaveBeenCalledTimes(1);
            expect(comp.show).toBe(false);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
        });

        it('should NOT have dot-content-type-layout', () => {
            const contentTypeLayout = de.query(By.css('dot-content-type-layout'));
            expect(contentTypeLayout === null).toBe(true);
        });

        it('should have show form by default', () => {
            const contentTypeForm = de.query(By.css('dot-content-types-form'));
            expect(contentTypeForm === null).toBe(false);
            expect(dialog === null).toBe(false);
        });

        it('should NOT have dot-content-type-fields-drop-zone', () => {
            const contentTypeForm = de.query(By.css('dot-content-type-fields-drop-zone'));
            expect(contentTypeForm === null).toBe(true);
        });

        it('should have create title create mode', () => {
            expect(dialog.componentInstance.header).toEqual('Create Content');
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
                spyOn(dotRouterService, 'goToEditContentType');

                const responseContentType = Object.assign({}, { id: '123' }, mockContentType, {
                    fields: [{ hello: 'world' }]
                });

                spyOn(crudService, 'postData').and.returnValue(observableOf([responseContentType]));
                spyOn(location, 'replaceState').and.returnValue(
                    observableOf([responseContentType])
                );

                contentTypeForm.triggerEventHandler('onSubmit', mockContentType);

                expect(crudService.postData).toHaveBeenCalledWith(
                    'v1/contenttype',
                    mockContentType
                );
                expect(comp.data).toEqual(responseContentType, 'set data with response');
                expect(comp.fields).toEqual(responseContentType.fields, 'ser fields with response');
                expect(dotRouterService.goToEditContentType).toHaveBeenCalledWith('123');
            });

            it('should handle error', () => {
                spyOn(crudService, 'postData').and.returnValue(
                    observableThrowError(mockResponseView(403))
                );
                spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();

                contentTypeForm.triggerEventHandler('onSubmit', mockContentType);
                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            });
        });

        describe('bind dialog actions to form', () => {
            let form: DebugElement;

            beforeEach(() => {
                form = de.query(By.css('dot-content-types-form'));
                spyOn(form.componentInstance, 'submitForm');
            });

            it('should bind save button disabled attribute to canSave property from the form', () => {
                form.triggerEventHandler('valid', true);
                expect(comp.dialogActions.accept.disabled).toBe(false);
            });

            it('should submit form when save button is clicked', () => {
                form.triggerEventHandler('valid', true);
                fixture.detectChanges();
                const saveButton = de.query(By.css('.dialog__button-accept'));
                saveButton.nativeElement.click();
                expect(form.componentInstance.submitForm).toHaveBeenCalledTimes(1);
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

    describe('edit mode', () => {
        beforeEach(async(() => {
            DOTTestBed.configureTestingModule(configEditMode);

            fixture = DOTTestBed.createComponent(ContentTypesEditComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            crudService = fixture.debugElement.injector.get(CrudService);
            location = fixture.debugElement.injector.get(Location);
            dotRouterService = fixture.debugElement.injector.get(DotRouterService);
            dotHttpErrorManagerService = fixture.debugElement.injector.get(
                DotHttpErrorManagerService
            );

            fixture.detectChanges();

            spyOn(comp, 'onDialogHide').and.callThrough();
            spyOn(dotRouterService, 'gotoPortlet');
        }));

        const clickEditButton = () => {
            const editButton: DebugElement = fixture.debugElement.query(
                By.css('#form-edit-button')
            );
            editButton.nativeNode.click();
            fixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));
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
            const editButton: DebugElement = fixture.debugElement.query(
                By.css('#form-edit-button')
            );
            expect(editButton.nativeElement.outerText).toBe('Edit');
            expect(editButton.nativeElement.disabled).toBe(false);
            expect(editButton).toBeTruthy();
        });

        it('should have edit content type title', () => {
            clickEditButton();
            expect(dialog.componentInstance.header).toEqual('Edit Content');
        });

        it('should open dialog on edit button click', () => {
            clickEditButton();

            expect(dialog).not.toBeNull();
            expect(comp.show).toBeTruthy();
            expect(dialog.componentInstance.visible).toBeTruthy();
        });

        it('should send notifications to add rows & tab divider', () => {
            const dotEventsService = fixture.debugElement.injector.get(DotEventsService);
            spyOn(dotEventsService, 'notify');

            comp.contentTypeActions[0].command();
            expect(comp.contentTypeActions[0].label).toBe('Add rows');
            expect(dotEventsService.notify).toHaveBeenCalledWith('add-row');

            comp.contentTypeActions[1].command();
            expect(comp.contentTypeActions[1].label).toBe('Add tab');
            expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
        });

        it('should close the dialog', () => {
            clickEditButton();

            const cancelButton = de.query(By.css('.dialog__button-cancel'));
            cancelButton.nativeElement.click();
            dialog.triggerEventHandler('hide', {});

            expect(comp.onDialogHide).toHaveBeenCalledTimes(1);
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

            const fieldsReturnByServer: ContentTypeField[] = newFieldsAdded.concat(
                currentFieldsInServer
            );
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(observableOf(fieldsReturnByServer));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

            // then: the saveFields method has to be called in FileService ...
            expect(fieldService.saveFields).toHaveBeenCalledWith('1234567890', newFieldsAdded);
        });

        it('should update fields on dropzone event when creating a new one or update', () => {
            const newFieldsAdded: ContentTypeField[] = [
                {
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                }
            ];

            const fieldsReturnByServer: ContentTypeField[] = newFieldsAdded.concat(
                currentFieldsInServer
            );
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(observableOf(fieldsReturnByServer));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);
            // ...and the comp.data.fields has to be set to the fields return by the service
            expect(comp.fields).toEqual(fieldsReturnByServer);
        });

        it('should handle 403 when user doesn\'t have permission to save feld', () => {
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
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            spyOn(fieldService, 'saveFields').and.returnValue(
                observableThrowError(mockResponseView(403))
            );

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        });

        it('should remove fields on dropzone event', () => {
            const fieldsReturnByServer: ContentTypeField[] = currentFieldsInServer.slice(-1);
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'deleteFields').and.returnValue(
                observableOf({ fields: fieldsReturnByServer })
            );

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

        it('should handle remove field error', () => {
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'deleteFields').and.returnValue(
                observableThrowError(mockResponseView(403))
            );

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            const fieldToRemove = {
                name: 'field 3',
                id: '3',
                clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                sortOrder: 3
            };

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.removeFields.emit(fieldToRemove);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
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

                spyOn(crudService, 'putData').and.returnValue(observableOf(responseContentType));

                contentTypeForm.triggerEventHandler('onSubmit', fakeContentType);

                expect(crudService.putData).toHaveBeenCalledWith(
                    'v1/contenttype/id/1234567890',
                    fakeContentType
                );
                expect(comp.data).toEqual(responseContentType, 'set data with response');
            });

            it('should handle error', () => {
                spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
                spyOn(crudService, 'putData').and.returnValue(
                    observableThrowError(mockResponseView(403))
                );

                contentTypeForm.triggerEventHandler('onSubmit', fakeContentType);

                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/content-types-angular');
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            });
        });
    });
});
