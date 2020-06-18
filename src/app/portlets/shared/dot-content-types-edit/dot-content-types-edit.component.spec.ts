import { throwError, of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DotContentTypesEditComponent } from './dot-content-types-edit.component';
import { DotCrudService } from '@services/dot-crud/dot-crud.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentType
} from 'dotcms-models';
import { FieldService } from './components/fields/service';
import { Location } from '@angular/common';
import { LoginService, SiteService } from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { async } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotContentTypesInfoService } from '@services/dot-content-types-info';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotMenuService } from '@services/dot-menu.service';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HotkeysService } from 'angular2-hotkeys';
import { TestHotkeysMock } from '../../../test/hotkeys-service.mock';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { MenuItem } from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotEditContentTypeCacheService } from './components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-edit-content-type-cache.service';
import { SiteServiceMock } from 'src/app/test/site-service.mock';
import * as _ from 'lodash';
import {
    dotcmsContentTypeFieldBasicMock,
    dotcmsContentTypeBasicMock
} from '@tests/dot-content-types.mock';

@Component({
    selector: 'dot-content-type-fields-drop-zone',
    template: ''
})
class TestContentTypeFieldsDropZoneComponent {
    @Input() layout: DotCMSContentTypeLayoutRow[];
    @Input() loading: boolean;
    @Output() saveFields = new EventEmitter<DotCMSContentTypeField[]>();
    @Output() removeFields = new EventEmitter<DotCMSContentTypeField[]>();

    cancelLastDragAndDrop(): void {}
}

@Component({
    selector: 'dot-content-type-layout',
    template: '<ng-content></ng-content>'
})
class TestContentTypeLayoutComponent {
    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-content-types-form',
    template: ''
})
class TestContentTypesFormComponent {
    @Input() data: DotCMSContentType;
    @Input() layout: DotCMSContentTypeField[];
    // tslint:disable-next-line:no-output-on-prefix
    @Output() onSubmit: EventEmitter<any> = new EventEmitter();

    resetForm = jasmine.createSpy('resetForm');

    submitForm(): void {}
}

@Component({
    selector: 'dot-menu',
    template: ''
})
export class TestDotMenuComponent {
    @Input() icon: string;
    @Input() float: boolean;
    @Input() model: MenuItem[];
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
            DotContentTypesEditComponent,
            TestContentTypeFieldsDropZoneComponent,
            TestContentTypesFormComponent,
            TestContentTypeLayoutComponent,
            TestDotMenuComponent
        ],
        imports: [
            RouterTestingModule.withRoutes([
                {
                    path: 'content-types-angular',
                    component: DotContentTypesEditComponent
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
                provide: SiteService,
                useClass: SiteServiceMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: ActivatedRoute,
                useValue: { data: of(route) }
            },
            {
                provide: HotkeysService,
                useValue: testHotKeysMock
            },
            DotCrudService,
            FieldService,
            DotContentTypesInfoService,
            DotMenuService,
            Location,
            DotEditContentTypeCacheService
        ]
    };
};

let comp: DotContentTypesEditComponent;
let fixture: ComponentFixture<DotContentTypesEditComponent>;
let de: DebugElement;
let crudService: DotCrudService;
let location: Location;
let dotRouterService: DotRouterService;
let dotHttpErrorManagerService: DotHttpErrorManagerService;
let testHotKeysMock: TestHotkeysMock;
let dialog: DebugElement;

describe('DotContentTypesEditComponent', () => {
    describe('create mode', () => {
        beforeEach(async(() => {
            testHotKeysMock = new TestHotkeysMock();
            const configCreateMode = getConfig({
                contentType: {
                    baseType: 'CONTENT'
                }
            });

            DOTTestBed.configureTestingModule(configCreateMode);

            fixture = DOTTestBed.createComponent(DotContentTypesEditComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            crudService = de.injector.get(DotCrudService);
            location = de.injector.get(Location);
            dotRouterService = de.injector.get(DotRouterService);
            dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);

            fixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));

            spyOn(comp, 'onDialogHide').and.callThrough();
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
            fixture.detectChanges();
            const portlet = dotRouterService.currentPortlet.id;

            expect(comp.onDialogHide).toHaveBeenCalledTimes(1);
            expect(comp.show).toBe(false);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith(`/${portlet}`);
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
            let mockContentType: DotCMSContentType;
            let contentTypeForm: DebugElement;

            beforeEach(() => {
                mockContentType = {
                    ...dotcmsContentTypeBasicMock,
                    clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
                    defaultType: false,
                    fixed: false,
                    folder: 'SYSTEM_FOLDER',
                    host: null,
                    name: 'Hello World',
                    system: false
                };

                contentTypeForm = de.query(By.css('dot-content-types-form'));
            });

            it('should create content type', () => {
                const responseContentType: DotCMSContentType = {
                    ...mockContentType,
                    ...{ id: '123' },
                    ...{
                        fields: [
                            {
                                ...dotcmsContentTypeFieldBasicMock,
                                name: 'hello world'
                            }
                        ],
                        layout: [
                            {
                                divider: {
                                    ...dotcmsContentTypeFieldBasicMock
                                },
                                columns: [
                                    {
                                        columnDivider: {
                                            ...dotcmsContentTypeFieldBasicMock
                                        },
                                        fields: [
                                            {
                                                ...dotcmsContentTypeFieldBasicMock,
                                                name: 'hello world'
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                };

                spyOn(crudService, 'postData').and.returnValue(of([responseContentType]));
                spyOn(location, 'replaceState').and.returnValue(of([responseContentType]));

                contentTypeForm.triggerEventHandler('onSubmit', mockContentType);

                const replacedWorkflowsPropContentType = {
                    ...mockContentType
                };

                (replacedWorkflowsPropContentType['workflow'] = mockContentType.workflows),
                    delete replacedWorkflowsPropContentType.workflows;

                expect(crudService.postData).toHaveBeenCalledWith(
                    'v1/contenttype',
                    replacedWorkflowsPropContentType
                );
                expect(comp.data).toEqual(responseContentType, 'set data with response');
                expect(comp.layout).toEqual(responseContentType.layout, 'ser fields with response');
                expect(dotRouterService.goToEditContentType).toHaveBeenCalledWith('123', dotRouterService.currentPortlet.id);
            });

            it('should handle error', () => {
                spyOn(crudService, 'postData').and.returnValue(throwError(mockResponseView(403)));
                spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();

                contentTypeForm.triggerEventHandler('onSubmit', mockContentType);
                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith(`/${dotRouterService.currentPortlet.id}`);
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            });

            it('should update workflows value', () => {
                spyOn(crudService, 'postData').and.returnValue(of([]));

                contentTypeForm.triggerEventHandler('onSubmit', {
                    workflows: [
                        {
                            id: '123',
                            name: 'Hello'
                        },
                        {
                            id: '456',
                            name: 'Work'
                        }
                    ]
                });

                expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', {
                    workflow: ['123', '456']
                });
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
            ...dotcmsContentTypeFieldBasicMock,
            name: 'fieldName',
            id: '4',
            clazz: 'fieldClass',
            sortOrder: 1
        },
        {
            ...dotcmsContentTypeFieldBasicMock,
            name: 'field 3',
            id: '3',
            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
            sortOrder: 3
        }
    ];

    const currentLayoutInServer: DotCMSContentTypeLayoutRow[] = [
        {
            divider: {
                ...dotcmsContentTypeFieldBasicMock
            },
            columns: [
                {
                    columnDivider: {
                        ...dotcmsContentTypeFieldBasicMock
                    },
                    fields: currentFieldsInServer
                }
            ]
        }
    ];

    const fakeContentType: DotCMSContentType = {
        ...dotcmsContentTypeBasicMock,
        baseType: 'CONTENT',
        id: '1234567890',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
        fields: currentFieldsInServer,
        layout: currentLayoutInServer,
        defaultType: true,
        fixed: true,
        folder: 'folder',
        host: 'host',
        name: 'name',
        owner: 'owner',
        system: false,
        variable: 'helloVariable'
    };

    const configEditMode = getConfig({
        contentType: fakeContentType
    });

    describe('edit mode', () => {
        beforeEach(async(() => {
            DOTTestBed.configureTestingModule(configEditMode);

            fixture = DOTTestBed.createComponent(DotContentTypesEditComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;

            crudService = fixture.debugElement.injector.get(DotCrudService);
            location = fixture.debugElement.injector.get(Location);
            dotRouterService = fixture.debugElement.injector.get(DotRouterService);
            dotHttpErrorManagerService = fixture.debugElement.injector.get(
                DotHttpErrorManagerService
            );

            fixture.detectChanges();

            spyOn(comp, 'onDialogHide').and.callThrough();
        }));

        const clickEditButton = () => {
            const contentTypeLayout = de.query(By.css('dot-content-type-layout'));
            contentTypeLayout.componentInstance.openEditDialog.next();
            fixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));
        };

        it('should set data, fields and  cache', () => {
            expect(comp.data).toBe(fakeContentType);
            expect(comp.layout).toBe(fakeContentType.layout);

            const dotEditContentTypeCacheService = de.injector.get(DotEditContentTypeCacheService);
            expect(dotEditContentTypeCacheService.get()).toEqual(fakeContentType);
        });

        it('should have dot-content-type-layout', () => {
            const contentTypeLayout = de.query(By.css('dot-content-type-layout'));
            expect(contentTypeLayout === null).toBe(false);
        });

        it('should have dot-content-type-fields-drop-zone', () => {
            const contentTypeForm = de.query(By.css('dot-content-type-fields-drop-zone'));
            expect(contentTypeForm === null).toBe(false);
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

            expect(comp.onDialogHide).toHaveBeenCalledTimes(1);
            expect(comp.show).toBe(false);
            expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
        });

        it('should update fields attribute when a field is edit', () => {
            const layout: DotCMSContentTypeLayoutRow[] = _.cloneDeep(currentLayoutInServer);
            const fieldToUpdate: DotCMSContentTypeField = layout[0].columns[0].fields[0];
            fieldToUpdate.name = 'Updated field';

            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(of(layout));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));
            contentTypeFieldsDropZone.componentInstance.saveFields.emit([fieldToUpdate]);

            expect(fieldService.saveFields).toHaveBeenCalledWith('1234567890', [fieldToUpdate]);
            expect(comp.layout).toEqual(layout);
        });

        it('should save fields on dropzone event', () => {
            const newFieldsAdded: DotCMSContentTypeField[] = [
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                },
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 2',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                    sortOrder: 2
                }
            ];

            const fieldsReturnByServer: DotCMSContentTypeField[] = newFieldsAdded.concat(
                currentFieldsInServer
            );
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(of(fieldsReturnByServer));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

            // then: the saveFields method has to be called in FileService ...
            expect(fieldService.saveFields).toHaveBeenCalledWith('1234567890', newFieldsAdded);
        });

        it('should show loading when saving fields on dropzone', () => {
            const newFieldsAdded: DotCMSContentTypeField[] = [
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                },
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 2',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                    sortOrder: 2
                }
            ];

            const fieldsReturnByServer: DotCMSContentTypeField[] = newFieldsAdded.concat(
                currentFieldsInServer
            );
            const fieldService = fixture.debugElement.injector.get(FieldService);

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            spyOn(fieldService, 'saveFields').and.callFake(() => {
                fixture.detectChanges();
                expect(contentTypeFieldsDropZone.componentInstance.loading).toBe(true);
                return of(fieldsReturnByServer);
            });

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

            fixture.detectChanges();
            expect(contentTypeFieldsDropZone.componentInstance.loading).toBe(false);
        });

        it('should update fields on dropzone event when creating a new one or update', () => {
            const newFieldsAdded: DotCMSContentTypeField[] = [
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                }
            ];

            const fieldsReturnByServer: DotCMSContentTypeLayoutRow[] = _.cloneDeep(
                currentLayoutInServer
            );
            newFieldsAdded.concat(fieldsReturnByServer[0].columns[0].fields);
            fieldsReturnByServer[0].columns[0].fields = newFieldsAdded;

            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(of(fieldsReturnByServer));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);
            // ...and the comp.data.fields has to be set to the fields return by the service
            expect(comp.layout).toEqual(fieldsReturnByServer);
        });

        it('should update fields on dropzone event when creating a new row and move a existing field', () => {
            const fieldsReturnByServer: DotCMSContentTypeField[] = currentFieldsInServer.map(
                (field) => {
                    const newfield = Object.assign({}, field);

                    if (!newfield.id) {
                        newfield.id = new Date().getMilliseconds().toString();
                    }

                    return newfield;
                }
            );

            const layout: DotCMSContentTypeLayoutRow[] = _.cloneDeep(currentLayoutInServer);
            layout[0].columns[0].fields = fieldsReturnByServer;
            layout[0].divider.id = new Date().getMilliseconds().toString();
            layout[0].columns[0].columnDivider.id = new Date().getMilliseconds().toString();

            const newRow: DotCMSContentTypeLayoutRow = {
                divider: {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                }
            };

            layout.push(newRow);

            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'saveFields').and.returnValue(of(layout));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(layout);
            // ...and the comp.data.fields has to be set to the fields return by the service
            expect(comp.layout).toEqual(layout);
        });

        it('should handle 403 when user doesn\'t have permission to save feld', () => {
            const dropZone = de.query(By.css('dot-content-type-fields-drop-zone'));
            spyOn(dropZone.componentInstance, 'cancelLastDragAndDrop').and.callThrough();

            const newFieldsAdded: DotCMSContentTypeField[] = [
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 1',
                    id: '1',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    sortOrder: 1
                },
                {
                    ...dotcmsContentTypeFieldBasicMock,
                    name: 'field 2',
                    id: '2',
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                    sortOrder: 2
                }
            ];
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            spyOn(fieldService, 'saveFields').and.returnValue(throwError(mockResponseView(403)));

            const contentTypeFieldsDropZone = de.query(By.css('dot-content-type-fields-drop-zone'));

            // when: the saveFields event is tiggered in content-type-fields-drop-zone
            contentTypeFieldsDropZone.componentInstance.saveFields.emit(newFieldsAdded);

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            expect(dropZone.componentInstance.cancelLastDragAndDrop).toHaveBeenCalledTimes(1);
        });

        it('should remove fields on dropzone event', () => {
            const layout: DotCMSContentTypeLayoutRow[] = _.cloneDeep(currentLayoutInServer);
            layout[0].columns[0].fields = layout[0].columns[0].fields.slice(-1);

            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'deleteFields').and.returnValue(of({ fields: layout }));

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
            expect(comp.layout).toEqual(layout);
        });

        it('should handle remove field error', () => {
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            const fieldService = fixture.debugElement.injector.get(FieldService);
            spyOn(fieldService, 'deleteFields').and.returnValue(throwError(mockResponseView(403)));

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

                spyOn(crudService, 'putData').and.returnValue(of(responseContentType));

                contentTypeForm.triggerEventHandler('onSubmit', fakeContentType);

                const replacedWorkflowsPropContentType = {
                    ...fakeContentType
                };

                (replacedWorkflowsPropContentType['workflow'] = fakeContentType.workflows),
                    delete replacedWorkflowsPropContentType.workflows;

                expect(crudService.putData).toHaveBeenCalledWith(
                    'v1/contenttype/id/1234567890',
                    replacedWorkflowsPropContentType
                );
                expect(comp.data).toEqual(responseContentType, 'set data with response');
            });

            it('should handle error', () => {
                spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
                spyOn(crudService, 'putData').and.returnValue(throwError(mockResponseView(403)));

                contentTypeForm.triggerEventHandler('onSubmit', fakeContentType);

                expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith(`/${dotRouterService.currentPortlet.id}`);
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            });
        });
    });
});
