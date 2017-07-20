import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { ContentTypesFormComponent } from './content-types-form.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, SimpleChange } from '@angular/core';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule } from 'primeng/primeng';
import { FieldValidationMessageModule } from '../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginService } from '../../../api/services/login-service';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { Observable } from 'rxjs/Observable';
import { ReactiveFormsModule } from '@angular/forms';
import { RequestMethod } from '@angular/http';
import { SiteSelectorModule } from '../../../view/components/_common/site-selector/site-selector.module';
import { SocketFactory } from '../../../api/services/protocol/socket-factory';

describe('ContentTypesFormComponent', () => {
    let comp: ContentTypesFormComponent;
    let fixture: ComponentFixture<ContentTypesFormComponent>;
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
            'name': 'Name',
            'save': 'Save',
            'update': 'Update'
        });

        DOTTestBed.configureTestingModule({
            declarations: [ ContentTypesFormComponent ],
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                DropdownModule,
                FieldValidationMessageModule,
                InputTextModule,
                OverlayPanelModule,
                ReactiveFormsModule,
                TabViewModule,
                SiteSelectorModule
            ],
            providers: [
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: MessageService, useValue: messageServiceMock },
                DotcmsConfig,
                SocketFactory
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type-form-form'));
        el = de.nativeElement;

        let changes: SimpleChange = new SimpleChange(null, null, true);
        comp.ngOnChanges({
            data: changes
        });
    }));

    it('should show workflow if the license its diferent to comunity(true)', async(() => {
        let dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);

        spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
            license: {isCommunity: true}
        }).delay(100));

        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            let workflowMsg = de.query(By.css('#field-workflow-hint'));
            expect(workflowMsg).not.toBeNull();
            expect(comp.form.get('workflow').disabled).toBeTruthy();

        });
    }));

    it('should show workflow if the license its diferent to comunity(false)', async(() => {
        let dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);

        spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
            license: {isCommunity: false}
        }).delay(100));

        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            let workflowMsg = de.query(By.css('#field-workflow-hint'));
            expect(workflowMsg).toBeNull();
            expect(comp.form.get('workflow').disabled).toBeFalsy();

        });
    }));

    it('should focus on the name field on load', async(() => {
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

    it('form should render basic fields for non-content types', async(() => {
        expect(Object.keys(comp.form.controls).length).toBe(4);
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflow')).not.toBeNull();
        expect(comp.form.get('detailPage')).toBeNull();
        expect(comp.form.get('urlMapPattern')).toBeNull();

        let fields = [
            '#content-type-form-description',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-workflow'
        ];

        fields.forEach(field => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    }));

    it('form should render extra fields for content types', () => {
        comp.ngOnChanges({
            type: new SimpleChange(null, 'content', true)
        });

        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(6);
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflow')).not.toBeNull();
        expect(comp.form.get('detailPage')).not.toBeNull();
        expect(comp.form.get('urlMapPattern')).not.toBeNull();

        let fields = [
            '#content-type-form-description',
            '#content-type-form-detail-page',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-url-map-pattern',
            '#content-type-form-workflow'
        ];

        fields.forEach(field => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('form should render dates fields in edit mode', () => {
        comp.data = {
            fields: [],
            hello: 'world'
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true),
            type: new SimpleChange(null, 'content', true)
        });

        fixture.detectChanges();

        expect(Object.keys(comp.form.controls).length).toBe(8);
        expect(comp.form.get('name')).not.toBeNull();
        expect(comp.form.get('host')).not.toBeNull();
        expect(comp.form.get('description')).not.toBeNull();
        expect(comp.form.get('workflow')).not.toBeNull();
        expect(comp.form.get('detailPage')).not.toBeNull();
        expect(comp.form.get('urlMapPattern')).not.toBeNull();
        expect(comp.form.get('publishDateVar')).not.toBeNull();
        expect(comp.form.get('expireDateVar')).not.toBeNull();

        let fields = [
            '#content-type-form-description',
            '#content-type-form-detail-page',
            '#content-type-form-host',
            '#content-type-form-name',
            '#content-type-form-url-map-pattern',
            '#content-type-form-workflow',
            '#content-type-form-publish-date-field',
            '#content-type-form-expire-date-field'
        ];

        fields.forEach(field => {
            expect(fixture.debugElement.query(By.css(field))).not.toBeNull();
        });
    });

    it('form should render disabled dates fields', () => {
        comp.data = {
            fields: [],
            hello: 'world'
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true),
            type: new SimpleChange(null, 'content', true)
        });

        expect(comp.form.get('publishDateVar').disabled).toBe(true);
        expect(comp.form.get('expireDateVar').disabled).toBe(true);
    });

    it('form should render enabled dates fields', () => {
        comp.data = {
            fields: [
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
            ],
            hello: 'world'
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true),
            type: new SimpleChange(null, 'content', true)
        });

        expect(comp.form.get('publishDateVar').disabled).toBe(false);
        expect(comp.form.get('expireDateVar').disabled).toBe(false);
    });

    it('form should not send data with invalid form', () => {
        comp.ngOnChanges({
            type: new SimpleChange(null, 'content', true)
        });

        let data = null;
        spyOn(comp, 'submitContent').and.callThrough();

        comp.onSubmit.subscribe(res => data = res);
        let submitFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-submit'));
        submitFormButton.nativeElement.click();

        expect(comp.submitContent).toHaveBeenCalledTimes(1);
        expect(data).toBeNull();
    });

    it('form should send data with valid form', () => {
        comp.ngOnChanges({
            type: new SimpleChange(null, 'content', true)
        });

        let data = null;
        spyOn(comp, 'submitContent').and.callThrough();

        comp.onSubmit.subscribe(res => data = res);

        comp.form.controls.name.setValue('A content type name');

        let submitFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-submit'));
        submitFormButton.nativeElement.click();

        expect(comp.submitContent).toHaveBeenCalledTimes(1);
        expect(data.value).toEqual({
            description: '',
            detailPage: '',
            host: '',
            name: 'A content type name',
            urlMapPattern: '',
            workflow: ''
        });
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
