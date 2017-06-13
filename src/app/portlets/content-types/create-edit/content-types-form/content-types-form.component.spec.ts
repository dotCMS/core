import { ActivatedRoute, Params, UrlSegment, ActivatedRouteSnapshot } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { ContentType } from '../main';
import { ContentTypesFormComponent } from './content-types-form.component';
import { ContentTypesInfoService } from '../../../../api/services/content-types-info';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement, SimpleChange } from '@angular/core';
import { DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule } from 'primeng/primeng';
import { FieldValidationMessageModule } from '../../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginService } from '../../../../api/services/login-service';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { CrudService } from '../../../../api/services/crud';

describe('ContentTypesFormComponent', () => {
    let comp: ContentTypesFormComponent;
    let fixture: ComponentFixture<ContentTypesFormComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    let route: ActivatedRoute;
    let params: Params;
    let url: Observable<UrlSegment[]>;

    beforeEach(async(() => {

        let messageServiceMock = new MockMessageService({
            'Content': 'Content',
            'Detail-Page': 'Detail Page',
            'Expire-Date-Field': 'Expire Date Field',
            'File': 'File',
            'Form': 'Form',
            'Host-Folder': 'Host or Folder',
            'Identifier': 'Identifier',
            'Page': 'Page',
            'Persona': 'Persona',
            'Properties': 'Properties',
            'Publish-Date-Field': 'Publish Date Field',
            'URL-Map-Pattern-hint1': 'Hello World',
            'URL-Pattern': 'URL Pattern',
            'Variable': 'Variable',
            'Widget': 'Widget',
            'Workflow': 'Workflow',
            'cancel': 'Cancel',
            'description': 'Description',
            'fields': 'Fields',
            'message.contentlet.required': 'This field is mandatory',
            'name': 'Name',
            'save': 'Save',
            'update': 'Update'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ ContentTypesFormComponent ],
            imports: [
                RouterTestingModule.withRoutes([{
                    component: ContentTypesFormComponent,
                    path: 'test'
                }]),
                BrowserAnimationsModule,
                ButtonModule,
                DropdownModule,
                FieldValidationMessageModule,
                InputTextModule,
                OverlayPanelModule,
                ReactiveFormsModule,
                TabViewModule
            ],
            providers: [
                { provide: MessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: ActivatedRoute, useValue: {}},
                CrudService,
                ContentTypesInfoService
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type-form-layout'));
        el = de.nativeElement;
        route = fixture.debugElement.injector.get(ActivatedRoute);

        let changes: SimpleChange = new SimpleChange(null, null, true);
        comp.ngOnChanges({
            data: changes
        });
    }));

    it('should focus on the name field on load', async(() => {
        let url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('content', { name: 'content' })
        ];
        route.url = Observable.of(url);

        let nameDebugEl: DebugElement = fixture.debugElement.query(By.css('#content-type-form-name'));
        spyOn(nameDebugEl.nativeElement, 'focus');

        fixture.detectChanges();

        expect(nameDebugEl.nativeElement.focus).toHaveBeenCalledTimes(1);
    }));

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

    it('should not send form with invalid data', () => {
        let url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('content', { name: 'content' })
        ];
        route.url = Observable.of(url);

        fixture.detectChanges();

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        let mockData: ContentType = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
            defaultType: false,
            description: '',
            detailPage: '',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: '',
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

    it('should send form with valid data to create content type', () => {
        let url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('content', { name: 'content' })
        ];

        route.url = Observable.of(url);
        comp.ngOnInit();

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
        expect(comp.form.controls.detailPage).toBeDefined();
        expect(comp.form.controls.urlMapPattern).toBeDefined();
        expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockData);
        expect(comp.readyToAddFields).toBeTruthy();
        expect(comp.submitAttempt).toBeFalsy();
    });

    it('should send form with valid data to create a file type', () => {
        let url = [
            new UrlSegment('create', { name: 'create' }),
            new UrlSegment('file', { name: 'file' })
        ];

        route.url = Observable.of(url);

        fixture.detectChanges();

        let crudService = fixture.debugElement.injector.get(CrudService);
        spyOn(crudService, 'postData').and.returnValue(Observable.of({}));

        let mockData: ContentType = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            defaultType: false,
            description: 'Hey I\'m a description',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-identifier',
            name: 'A file type name',
            owner: '123',
            system: false,
            variable: 'aFileTypeName',
            workflow: 'workflow-identifier'
        };

        comp.form.controls.name.setValue('A file type name');
        comp.form.controls.description.setValue('Hey I\'m a description');
        comp.form.controls.workflow.setValue('workflow-identifier');
        comp.form.controls.host.setValue('host-identifier');
        comp.submitContent();
        expect(comp.form.controls.detailPage).toBeUndefined();
        expect(comp.form.controls.urlMapPattern).toBeUndefined();
        expect(crudService.postData).toHaveBeenCalledWith('v1/contenttype', mockData);
        expect(comp.readyToAddFields).toBeTruthy();
        expect(comp.submitAttempt).toBeFalsy();
    });

    it('should edit and send form with valid data', () => {
        let url = [
            new UrlSegment('edit', { name: 'edit' }),
            new UrlSegment('file', { name: 'file' })
        ];

        // The isEditMode needs data and that is set until the second
        // ngOnChanges, that's why we delay this a little bit
        route.url = Observable.of(url).delay(100);

        let snapshot: ActivatedRouteSnapshot = new ActivatedRouteSnapshot();
        route.snapshot = snapshot;
        route.snapshot.url = url;

        let data: any = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            defaultType: false,
            description: 'Hey I\'m a description',
            fields: [],
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-identifier',
            iDate: 1496963858000,
            id: '1234567890',
            modDate: 1496967718000,
            multilingualable: false,
            name: 'A content type name',
            owner: '123',
            system: false,
            variable: 'ContentTypeName',
            versionable: true
        };

        let mockData: ContentType = {
            clazz: 'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
            defaultType: false,
            description: 'Hey I\'m a description',
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: 'host-identifier',
            id: '1234567890',
            name: 'Edited Name',
            owner: '123',
            system: false,
            variable: 'ContentTypeName',
            workflow: ''
        };

        comp.data = data;
        let changes: SimpleChange = new SimpleChange(null, data, false);
        comp.ngOnChanges({
            data: changes
        });

        comp.form.controls.name.setValue('Edited Name');

        // Because the delay in the route.url we need to delay the
        // tests until everything it's ready.
        // Try to remove this when I separate create/edit components.
        setTimeout(() => {
            let crudService = fixture.debugElement.injector.get(CrudService);
            spyOn(crudService, 'putData').and.returnValue(Observable.of({}));
            comp.submitContent();
            expect(comp.form.controls.detailPage).toBeUndefined();
            expect(comp.form.controls.urlMapPattern).toBeUndefined();
            expect(crudService.putData).toHaveBeenCalledWith('v1/contenttype/id/1234567890', mockData);
            expect(comp.readyToAddFields).toBeTruthy();
            expect(comp.submitAttempt).toBeFalsy();
        }, 150);
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
