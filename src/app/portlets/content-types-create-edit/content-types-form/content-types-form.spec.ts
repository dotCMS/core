import { LoginService } from '../../../api/services/login-service';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { ContentType } from '../content-types-create-edit-component';
import { ContentTypesForm } from './content-types-form';
import { CrudService } from '../../../api/services/crud-service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule } from 'primeng/primeng';
import { FieldValidationMessageComponent } from '../../../view/components/_common/field-validation-message/field-validation-message';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SiteService } from '../../../api/services/site-service';
import { SiteServiceMock } from '../../../test/site-service.mock';
import { LoginServiceMock } from '../../../test/login-service.mock';

let routerMock = {
    navigate: jasmine.createSpy('navigate')
};

describe('Content Type Form Component', () => {
    let comp: ContentTypesForm;
    let fixture: ComponentFixture<ContentTypesForm>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(async(() => {
        let messageServiceMock = new MockMessageService({
            'Detail-Page': 'Detail Page',
            'Expire-Date-Field': 'Expire Date Field',
            'Host-Folder': 'Host or Folder',
            'Identifier': 'Identifier',
            'Properties': 'Properties',
            'Publish-Date-Field': 'Publish Date Field',
            'URL-Map-Pattern-hint1': 'Hello World',
            'URL-Pattern': 'URL Pattern',
            'Variable': 'Variable',
            'Workflow': 'Workflow',
            'cancel': 'Cancel',
            'description': 'Description',
            'fields': 'Fields',
            'message.contentlet.required': 'This field is mandatory',
            'save': 'Save'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ ContentTypesForm, FieldValidationMessageComponent ],
            imports: [ ReactiveFormsModule, DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule ],
            providers: [
                { provide: Router, useValue: routerMock },
                { provide: SiteService, useClass: SiteServiceMock },
                { provide: MessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                CrudService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesForm);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type-form-layout'));
        el = de.nativeElement;
        comp.ngOnInit();
    }));

    it('should focus on the name field on load', () => {
        let nameDebugEl: DebugElement = fixture.debugElement.query(By.css('#content-type-form-name'));
        spyOn(nameDebugEl.nativeElement, 'focus');
        fixture.detectChanges();
        expect(nameDebugEl.nativeElement.focus).toHaveBeenCalledTimes(1);
    });

    it('should have a button to expand/collapse the form', () => {
        let expandFormButton: DebugElement = fixture.debugElement.query(By.css('#custom-type-form-expand-button'));
        expect(expandFormButton).toBeDefined();
    });

    it('should call toogleForm method on action button click', () => {
        spyOn(comp, 'toggleForm');
        let expandFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-expand-button'));
        expandFormButton.triggerEventHandler('click', null);
        expect(comp.toggleForm).toHaveBeenCalledTimes(1);
    });

    it('should toggle formState property on action button click', () => {
        let expandFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-expand-button'));
        expandFormButton.triggerEventHandler('click', null);
        expect(comp.formState).toBe('expanded');
        expandFormButton.triggerEventHandler('click', null);
        expect(comp.formState).toBe('collapsed');
    });

    it('form should be invalid by default', () => {
        expect(comp.form.valid).toBeFalsy();
    });

    it('should not send form with invalid data ', () => {
        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        let mockData: ContentType = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            description: '',
            detailPage: '',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '123-xyz-567-xxl',
            name: '',
            owner: '123',
            system: false,
            urlMapPattern: '',
            variable: '',
            workflow: ''
        };

        comp.submitContent();
        expect(comp.form.valid).toBeFalsy();
        expect(comp.formData).toEqual(mockData);
        expect(crudService.postData).not.toHaveBeenCalled();
    });

    it('should send form with valid data and set fields section', () => {
        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        let mockData: ContentType = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            description: 'Hey I\'m a description',
            detailPage: 'detail.html',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-identifier',
            name: 'A content type name',
            owner: '123',
            system: false,
            urlMapPattern: 'pattern',
            variable: 'aContentTypeName',
            workflow: 'workflow-identifier'
        };

        comp.form.controls.name.setValue('A content type name');
        comp.form.controls.description.setValue('Hey I\'m a description');
        comp.form.controls.detailPage.setValue('detail.html');
        comp.form.controls.urlMapPattern.setValue('pattern');
        comp.form.controls.workflow.setValue('workflow-identifier');
        comp.form.controls.host.setValue('host-identifier');
        comp.submitContent();
        expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockData);
        expect(comp.readyToAddFields).toBeTruthy();
        expect(comp.submitAttempt).toBeFalsy();
    });

    xit('should have full form collapsed by default', () => {
        // TODO: Needs to figute it out why by default the offsetHeight it's not 0
        fixture.detectChanges();
        let fullForm: DebugElement = fixture.debugElement.query(By.css('#content-type-form-full'));
        let fullFormEl: HTMLElement = fullForm.nativeElement;
        expect(fullFormEl.offsetHeight).toBe(0);
    });

    xit('should expand full form action button click', () => {
        // TODO: Needs to fix previous test
        let fullForm: DebugElement = fixture.debugElement.query(By.css('#content-type-form-full'));
        let fullFormEl: HTMLElement = fullForm.nativeElement;
        let expandFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-expand-button'));
        expandFormButton.triggerEventHandler('click', null);
        fixture.detectChanges();
        expect(fullFormEl.offsetHeight).toBeGreaterThan(0);
    });

});