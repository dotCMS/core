import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { Observable } from 'rxjs/Observable';

import { ContentTypesFormComponent } from './content-types-form.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotcmsConfig, LoginService } from 'dotcms-js/dotcms-js';
import { DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule } from 'primeng/primeng';
import { FieldValidationMessageModule } from '../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { SiteSelectorFieldModule } from '../../../view/components/_common/site-selector-field/site-selector-field.module';
import { SiteService } from 'dotcms-js/dotcms-js';
import { SiteServiceMock } from '../../../test/site-service.mock';
import { WorkflowService } from '../../../api/services/workflow/workflow.service';
import { RouterTestingModule } from '@angular/router/testing';
import { MdInputTextModule } from '../../../view/directives/md-inputtext/md-input-text.module';

describe('ContentTypesFormComponent', () => {
    let comp: ContentTypesFormComponent;
    let fixture: ComponentFixture<ContentTypesFormComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let dotcmsConfig: DotcmsConfig;

    beforeEach(
        async(() => {
            const messageServiceMock = new MockDotMessageService({
                'contenttypes.form.field.detail.page': 'Detail Page',
                'contenttypes.form.field.expire.date.field': 'Expire Date Field',
                'contenttypes.form.field.host_folder.label': 'Host or Folder',
                'contenttypes.form.identifier': 'Identifier',
                'contenttypes.form.label.publish.date.field': 'Publish Date Field',
                'contenttypes.hint.URL.map.pattern.hint1': 'Hello World',
                'contenttypes.form.label.URL.pattern': 'URL Pattern',
                'contenttypes.content.variable': 'Variable',
                'contenttypes.form.label.workflow': 'Workflow',
                'contenttypes.action.cancel': 'Cancel',
                'contenttypes.form.label.description': 'Description',
                'contenttypes.form.name': 'Name',
                'contenttypes.action.save': 'Save',
                'contenttypes.action.update': 'Update',
                'contenttypes.action.create': 'Create',
                'contenttypes.action.edit': 'Edit',
                'contenttypes.action.delete': 'Delete',
                'contenttypes.form.name.error.required': 'Error is wrong',
                'contenttypes.action.form.cancel': 'Cancel',
                'contenttypes.content.contenttype': 'content type',
                'contenttypes.content.fileasset': 'fileasset',
                'contenttypes.content.content': 'Content',
                'contenttypes.content.form': 'Form',
                'contenttypes.content.persona': 'Persona',
                'contenttypes.content.widget': 'Widget',
                'contenttypes.content.htmlpage': 'Page',
                'contenttypes.content.key_value': 'Key Value',
                'contenttypes.content.vanity_url:': 'Vanity Url'
            });

            const siteServiceMock = new SiteServiceMock();

            DOTTestBed.configureTestingModule({
                declarations: [ContentTypesFormComponent],
                imports: [
                    RouterTestingModule.withRoutes([{ component: ContentTypesFormComponent, path: 'test' }]),
                    BrowserAnimationsModule,
                    ButtonModule,
                    DropdownModule,
                    FieldValidationMessageModule,
                    InputTextModule,
                    OverlayPanelModule,
                    ReactiveFormsModule,
                    TabViewModule,
                    SiteSelectorFieldModule,
                    RouterTestingModule,
                    MdInputTextModule
                ],
                providers: [
                    { provide: LoginService, useClass: LoginServiceMock },
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: SiteService, useValue: siteServiceMock },
                    DotcmsConfig,
                    ContentTypesInfoService,
                    WorkflowService
                ]
            });

            fixture = DOTTestBed.createComponent(ContentTypesFormComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;

            dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);
        })
    );

    it('should be invalid by default', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();
        expect(comp.form.valid).toBeFalsy();
    });

    it('should be valid when name field have value', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue('content type name');
        expect(comp.form.valid).toBeTruthy();
    });

    it('should have name focus by default on create mode', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();
        expect(comp.name.nativeElement).toBe(document.activeElement);
    });

    it('should have canSave property false by default (form is invalid)', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should set canSave property true form is valid', () => {
        comp.data = {
            name: 'hello',
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        // Form is only valid when "name" property is set
        comp.form.get('description').setValue('hello world');
        fixture.detectChanges();

        expect(comp.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid in edit mode', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue(null);
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should set canSave property true when form is valid and model updated in edit mode', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('description').setValue('some desc');
        fixture.detectChanges();

        expect(comp.canSave).toBe(true);
    });

    it('should set canSave property false when form is invalid and model updated in edit mode', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();

        comp.form.get('name').setValue(null);
        comp.form.get('description').setValue('some desc');
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    // tslint:disable-next-line:max-line-length
    it('should set canSave property false when the form value is updated and then gets back to the original content (no community license)', () => {
        spyOn(dotcmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: { isCommunity: false }
            })
        );

        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');

        comp.form.get('name').setValue('A new  name');
        fixture.detectChanges();
        expect(comp.canSave).toBe(true, 'name updated set it to true');

        comp.form.get('name').setValue('Hello World');
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'revert the change button disabled set it to false');
    });

    // tslint:disable-next-line:max-line-length
    it('should set canSave property false when the form value is updated and then gets back to the original content (community license)', () => {
        spyOn(dotcmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: { isCommunity: true }
            })
        );

        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');

        comp.form.get('name').setValue('A new  name');
        fixture.detectChanges();
        expect(comp.canSave).toBe(true, 'name updated set it to true');

        comp.form.get('name').setValue('Hello World');
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'revert the change button disabled set it to false');
    });

    it('should set canSave property false when edit a content with fields', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123',
            name: 'Hello World'
        };
        comp.fields = [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                id: '123',
                indexed: true,
                name: 'Date 1'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                id: '456',
                indexed: true,
                name: 'Date 2'
            }
        ];
        fixture.detectChanges();
        expect(comp.canSave).toBe(false, 'by default is false');
    });

    it('should set canSave property false on edit mode', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123'
        };
        fixture.detectChanges();

        expect(comp.canSave).toBe(false);
    });

    it('should have basic form controls for non-content base types', () => {
        comp.data = {
            baseType: 'WIDGET'
        };
        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(11);
        expect(comp.form.get('clazz')).not.toBeNull();
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflow')).not.toBeNull();
        expect(comp.form.get('publishDateVar')).not.toBeNull();
        expect(comp.form.get('expireDateVar')).not.toBeNull();
        expect(comp.form.get('defaultType')).not.toBeNull();
        expect(comp.form.get('fixed')).not.toBeNull();
        expect(comp.form.get('system')).not.toBeNull();
        expect(comp.form.get('folder')).not.toBeNull();

        expect(comp.form.get('detailPage')).toBeNull();
        expect(comp.form.get('urlMapPattern')).toBeNull();
    });

    it('should render basic fields for non-content base types', () => {
        comp.data = {
            baseType: 'WIDGET'
        };
        fixture.detectChanges();

        const fields = [
            '#content-type-form-description',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-workflow',
            '#content-type-form-publish-date-field',
            '#content-type-form-expire-date-field'
        ];

        fields.forEach((field) => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('should have basic form controls for content base type', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(13);
        expect(comp.form.get('clazz')).not.toBeNull();
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflow')).not.toBeNull();
        expect(comp.form.get('publishDateVar')).not.toBeNull();
        expect(comp.form.get('expireDateVar')).not.toBeNull();
        expect(comp.form.get('detailPage')).not.toBeNull();
        expect(comp.form.get('urlMapPattern')).not.toBeNull();
        expect(comp.form.get('defaultType')).not.toBeNull();
        expect(comp.form.get('fixed')).not.toBeNull();
        expect(comp.form.get('system')).not.toBeNull();
        expect(comp.form.get('folder')).not.toBeNull();
    });

    it('should render extra fields for content types', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        const fields = [
            '#content-type-form-description',
            '#content-type-form-detail-page',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-url-map-pattern',
            '#content-type-form-workflow',
            '#content-type-form-detail-page',
            '#content-type-form-url-map-pattern'
        ];

        fields.forEach((field) => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('should show workflow disabled and with message if the license community its true', () => {
        spyOn(dotcmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: { isCommunity: true }
            })
        );

        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        const workflowMsg = de.query(By.css('#field-workflow-hint'));
        expect(workflowMsg).toBeTruthy();
        expect(comp.form.get('workflow').disabled).toBeTruthy();
    });

    it('should show workflow enable and no message if the license community its false', () => {
        spyOn(dotcmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: { isCommunity: false }
            })
        );

        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        const workflowMsg = de.query(By.css('#field-workflow-hint'));
        expect(workflowMsg).toBeFalsy();
        expect(comp.form.get('workflow').disabled).toBeFalsy();
    });

    it('should call the WorkFlow endpoint if the license community its false', () => {
        const workflowService: WorkflowService = fixture.debugElement.injector.get(WorkflowService);
        spyOn(dotcmsConfig, 'getConfig').and.returnValue(
            Observable.of({
                license: { isCommunity: false }
            })
        );
        spyOn(workflowService, 'get').and.returnValue(Observable.of([{ id: '123' }]));
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();
        expect(workflowService.get).toHaveBeenCalled();
    });

    it('should render disabled dates fields and hint when date fields are not passed', () => {
        comp.data = {
            baseType: 'CONTENT',
            id: '123'
        };
        fixture.detectChanges();

        const dateFieldMsg = de.query(By.css('#field-dates-hint'));

        expect(dateFieldMsg).toBeTruthy();
        expect(comp.form.get('publishDateVar').disabled).toBe(true);
        expect(comp.form.get('expireDateVar').disabled).toBe(true);
    });

    it('should render enabled dates fields when date fields are passed', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        comp.fields = [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                id: '123',
                indexed: true,
                name: 'Date 1'
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                id: '456',
                indexed: true,
                name: 'Date 2'
            }
        ];
        fixture.detectChanges();

        expect(comp.form.get('publishDateVar').disabled).toBe(false);
        expect(comp.form.get('expireDateVar').disabled).toBe(false);
    });

    it('should not submit form with invalid form', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        let data = null;
        spyOn(comp, 'submitForm').and.callThrough();

        comp.submit.subscribe((res) => (data = res));
        comp.submitForm();

        expect(comp.submitForm).toHaveBeenCalled();
        expect(data).toBeNull();
    });

    it('should send data with valid form', () => {
        comp.data = {
            baseType: 'CONTENT'
        };
        fixture.detectChanges();

        let data = null;
        spyOn(comp, 'submitForm').and.callThrough();

        comp.submit.subscribe((res) => (data = res));

        comp.form.controls.name.setValue('A content type name');

        fixture.detectChanges();

        comp.submitForm();

        expect(comp.submitForm).toHaveBeenCalledTimes(1);
        expect(data).toEqual({
            clazz: '',
            description: '',
            detailPage: '',
            host: '',
            name: 'A content type name',
            urlMapPattern: '',
            defaultType: null,
            fixed: null,
            folder: null,
            system: null
        });
    });
});
