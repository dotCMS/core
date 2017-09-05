import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async, fakeAsync, tick } from '@angular/core/testing';
import { DebugElement, SimpleChange } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RequestMethod } from '@angular/http';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { SocketFactory } from 'dotcms-js/core/socket-factory.service';

import { ContentTypesFormComponent } from './content-types-form.component';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotcmsConfig, LoginService } from 'dotcms-js/dotcms-js';
import { DropdownModule, OverlayPanelModule, ButtonModule, InputTextModule, TabViewModule } from 'primeng/primeng';
import { FieldValidationMessageModule } from '../../../view/components/_common/field-validation-message/file-validation-message.module';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MessageService } from '../../../api/services/messages-service';
import { MockMessageService } from '../../../test/message-service.mock';
import { SiteSelectorModule } from '../../../view/components/_common/site-selector/site-selector.module';

describe('ContentTypesFormComponent', () => {
    let comp: ContentTypesFormComponent;
    let fixture: ComponentFixture<ContentTypesFormComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let deleteAction: any;
    const mockRouter = {
        navigate: jasmine.createSpy('navigate')
    };

    beforeEach(async(() => {

        const messageServiceMock = new MockMessageService({
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
            'contenttypes.action.update': 'Update'
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
                { provide: ActivatedRoute, useValue: {'params': Observable.from([{ id: '1234' }])} },
                { provide: Router, useValue: mockRouter },
                DotcmsConfig,
                SocketFactory
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFormComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('#content-type-form-form'));
        el = de.nativeElement;

        const changes: SimpleChange = new SimpleChange(null, null, true);
        comp.ngOnChanges({
            data: changes
        });
    }));

    it('should show workflow if the license its diferent to comunity(true)', async(() => {
        const dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);

        spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
            license: {isCommunity: true}
        }).delay(100));

        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const workflowMsg = de.query(By.css('#field-workflow-hint'));
            expect(workflowMsg).not.toBeNull();
            expect(comp.form.get('workflow').disabled).toBeTruthy();

        });
    }));

    it('should show workflow if the license its diferent to comunity(false)', async(() => {
        const dotcmsConfig = fixture.debugElement.injector.get(DotcmsConfig);

        spyOn(dotcmsConfig, 'getConfig').and.returnValue(Observable.of({
            license: {isCommunity: false}
        }).delay(100));

        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const workflowMsg = de.query(By.css('#field-workflow-hint'));
            expect(workflowMsg).toBeNull();
            expect(comp.form.get('workflow').disabled).toBeFalsy();

        });
    }));

    it('should have a button to expand/collapse the form', () => {
        const expandFormButton: DebugElement = fixture.debugElement.query(By.css('#custom-type-form-expand-button'));
        expect(expandFormButton).toBeDefined();
    });

    it('should call toogleForm method on action button click', () => {
        spyOn(comp, 'toggleForm');
        comp.data = {
            fields: [
                {
                    dataType: 'DATE_TIME',
                    id: '123',
                    indexed: true,
                    name: 'Date 1'
                }
            ]
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true)
        });
        fixture.detectChanges();
        const expandFormEditButton: DebugElement = fixture
            .debugElement.query(By.css('.content-type__form-actions p-splitButton .ui-menu-list .ui-menuitem:first-child a'));

        expandFormEditButton.nativeNode.click();
        expect(comp.toggleForm).toHaveBeenCalledTimes(1);
    });

    it('should call delete method on action button click', () => {
        comp.data = {
            fields: [
                {
                    dataType: 'DATE_TIME',
                    id: '123',
                    indexed: true,
                    name: 'Date 1'
                }
            ]
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true)
        });
        comp.onDelete.subscribe(() => this.action = true);
        fixture.detectChanges();

        const expandFormDeleteButton: DebugElement = fixture
            .debugElement.query(By.css('.content-type__form-actions p-splitButton .ui-menu-list .ui-menuitem:nth-child(2) a'));

        expandFormDeleteButton.nativeNode.click();
        expect(true).toBe(this.action);
    });

    it('should toggle formState property on action button click', () => {
        comp.data = {
            fields: [
                {
                    dataType: 'DATE_TIME',
                    id: '123',
                    indexed: true,
                    name: 'Date 1'
                }
            ]
        };
        comp.ngOnChanges({
            data: new SimpleChange(null, comp.data, true)
        });
        fixture.detectChanges();
        const expandFormButton: DebugElement = fixture
            .debugElement.query(By.css('.content-type__form-actions p-splitButton .ui-menu-list .ui-menuitem:first-child a'));
        expandFormButton.nativeNode.click();
        expect(comp.formState).toBe('expanded');
        expandFormButton.nativeNode.click();
        expect(comp.formState).toBe('collapsed');
    });

    it('should toggle formState when the user focus on the name field', async(() => {
        const nameDebugEl: DebugElement = fixture.debugElement.query(By.css('#content-type-form-name'));
        spyOn(nameDebugEl.nativeElement, 'focus');
        nameDebugEl.nativeNode.focus();
        fixture.detectChanges();
        expect(nameDebugEl.nativeElement.focus).toHaveBeenCalledTimes(1);
        expect(comp.formState).toBe('collapsed');
    }));

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

        const fields = [
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

        const fields = [
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

        const fields = [
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
        const submitFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-submit'));
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

        const submitFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-submit'));
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
        const fullForm: DebugElement = fixture.debugElement.query(By.css('#content-type-form-full'));
        const fullFormEl: HTMLElement = fullForm.nativeElement;
        expect(fullFormEl.offsetHeight).toBe(0);
    });

    xit('should expand full form action button click', () => {
        // TODO: Needs to fix previous test
        const fullForm: DebugElement = fixture.debugElement.query(By.css('#content-type-form-full'));
        const fullFormEl: HTMLElement = fullForm.nativeElement;
        const expandFormButton: DebugElement = fixture.debugElement.query(By.css('#content-type-form-expand-button'));
        expandFormButton.triggerEventHandler('click', null);
        fixture.detectChanges();
        expect(fullFormEl.offsetHeight).toBeGreaterThan(0);
    });

});
