import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotPushPublishFormComponent } from './dot-push-publish-form.component';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { Component, DebugElement, Input } from '@angular/core';
import { PushPublishServiceMock } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
    CalendarModule,
    ConfirmationService,
    Dropdown,
    DropdownModule,
    SelectButton,
    SelectButtonModule
} from 'primeng/primeng';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { PushPublishEnvSelectorModule } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import {
    DotPushPublishFilter,
    DotPushPublishFiltersService
} from '@services/dot-push-publish-filters/dot-push-publish-filters.service';
import { CoreWebService, LoginService } from 'dotcms-js';
import { CoreWebServiceMock } from '../../../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotParseHtmlService } from '@services/dot-parse-html/dot-parse-html.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { LoginServiceMock } from '@tests/login-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { By } from '@angular/platform-browser';
import { DotPushPublishDialogData } from 'dotcms-models';
import { SelectItem } from 'primeng/api';
import { PushPublishEnvSelectorComponent } from '@components/_common/dot-push-publish-env-selector/dot-push-publish-env-selector.component';
import { of } from 'rxjs';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.content.push_publish.action.push': 'Push',
    'contenttypes.content.push_publish.action.remove': 'Remove',
    'contenttypes.content.push_publish.action.pushremove': 'Push Remove',
    'contenttypes.content.push_publish.push_to_errormsg': 'Must add at least one Environment',
    'contenttypes.content.push_publish.publish_date_errormsg': 'Publish Date is required',
    'contenttypes.content.push_publish.expire_date_errormsg': 'Expire Date is required'
});

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-push-publish-form *ngIf="data" (valid)="valid = $event" (value)="value = $event" [data]="data"></dot-push-publish-form>'
})
class TestHostComponent {
    @Input() data: DotPushPublishDialogData;
    valid: boolean;
    value: any;
}

const mockPublishFormData: DotPushPublishDialogData = {
    assetIdentifier: '123',
    title: 'Test Title'
};

const mockFilters: DotPushPublishFilter[] = [
    { defaultFilter: true, key: 'key1', title: 'Title default' },
    { defaultFilter: false, key: 'key2', title: 'Title key2' },
    { defaultFilter: false, key: 'key3', title: 'A - Tittle' }
];

const mockSortedFilters: SelectItem[] = [
    { label: 'A - Tittle', value: 'key3' },
    { label: 'Title default', value: 'key1' },
    { label: 'Title key2', value: 'key2' }
];

const mockPushActions: SelectItem[] = [
    {
        label: 'Push',
        value: 'publish'
    },
    {
        label: 'Remove',
        value: 'expire',
        disabled: false
    },
    {
        label: 'Push Remove',
        value: 'publishexpire',
        disabled: false
    }
];

const mockDate = new Date('2020, 8, 14');

describe('DotPushPublishFormComponent', () => {
    let hostComponent: TestHostComponent;
    let pushPublishForm: DotPushPublishFormComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let pushPublishServiceMock: PushPublishServiceMock;
    let dotPushPublishFiltersService: DotPushPublishFiltersService;
    let pushActionsSelect: SelectButton;
    let selectActionButtons: DebugElement[];

    const mockFormInitialValue = {
        filterKey: mockFilters[0].key,
        pushActionSelected: mockPushActions[0].value,
        publishDate: mockDate,
        environment: ''
    };

    beforeEach(
        async(() => {
            pushPublishServiceMock = new PushPublishServiceMock();
            TestBed.configureTestingModule({
                declarations: [DotPushPublishFormComponent, TestHostComponent],
                providers: [
                    { provide: PushPublishService, useValue: pushPublishServiceMock },
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: ConnectionBackend, useClass: MockBackend },
                    { provide: RequestOptions, useClass: BaseRequestOptions },
                    { provide: LoginService, useClass: LoginServiceMock },
                    { provide: DotRouterService, useClass: MockDotRouterService },
                    DotPushPublishFiltersService,
                    Http,
                    DotParseHtmlService,
                    DotHttpErrorManagerService,
                    DotAlertConfirmService,
                    ConfirmationService
                ],
                imports: [
                    FormsModule,
                    CalendarModule,
                    DotDialogModule,
                    PushPublishEnvSelectorModule,
                    ReactiveFormsModule,
                    DropdownModule,
                    DotFieldValidationMessageModule,
                    SelectButtonModule,
                    DotPipesModule
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        jasmine.clock().install();
        jasmine.clock().mockDate(mockDate);
        fixture = TestBed.createComponent(TestHostComponent);
        dotPushPublishFiltersService = fixture.debugElement.injector.get(
            DotPushPublishFiltersService
        );
        hostComponent = fixture.componentInstance;
        spyOn(dotPushPublishFiltersService, 'get').and.returnValue(of(mockFilters));
        hostComponent.data = mockPublishFormData;
        fixture.detectChanges();
        pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
            .componentInstance;
        pushActionsSelect = fixture.debugElement.query(By.css('p-selectButton')).componentInstance;
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it('should load filters on load', () => {
        const filterDropDown: Dropdown = fixture.debugElement.query(By.css('p-dropdown'))
            .componentInstance;

        expect(filterDropDown.options).toEqual(mockSortedFilters);
    });

    it('should pass assetIdentifier to dot-push-publish-env-selector', () => {
        const pushPublishEnvSelectorComponent: PushPublishEnvSelectorComponent = fixture.debugElement.query(
            By.css('dot-push-publish-env-selector')
        ).componentInstance;

        expect(pushPublishEnvSelectorComponent.assetIdentifier).toEqual(
            mockPublishFormData.assetIdentifier
        );
    });

    it('should load PushPublish Actions', () => {
        expect(pushActionsSelect.options).toEqual(mockPushActions);
    });

    it('should set form value of load and emit invalid', () => {
        expect(hostComponent).toBeTruthy();
        expect(pushPublishForm.form.value).toEqual(mockFormInitialValue);
        expect(hostComponent.valid).toEqual(false); // Environment not selected.
        expect(hostComponent.value).toEqual(mockFormInitialValue);
    });

    describe('Push Action scenarios', () => {
        beforeEach(() => {
            selectActionButtons = fixture.debugElement.queryAll(
                By.css('p-selectButton .ui-button')
            );
        });

        it('should disable publish date on select remove', () => {
            selectActionButtons[1].triggerEventHandler('click', {});
            expect(pushPublishForm.form.value.publishDate).toBeUndefined();
        });

        it('should enable both date fields on select push-remove', () => {
            selectActionButtons[2].triggerEventHandler('click', {});
            expect(pushPublishForm.form.value.publishDate).toEqual(mockDate);
            expect(pushPublishForm.form.value.expireDate).toEqual(mockDate);
        });

        it('should disable expired date on select push', () => {
            selectActionButtons[0].triggerEventHandler('click', {});
            expect(pushPublishForm.form.value.expireDate).toBeUndefined();
        });

        it('should disable publish expired on removeOnly data ', () => {
            hostComponent.data = null;
            fixture.detectChanges();
            hostComponent.data = { removeOnly: true, ...mockPublishFormData };
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
                .componentInstance;
            expect(pushPublishForm.pushActions[2].disabled).toEqual(true);
        });

        it('should disable remove and publish expired on restricted data ', () => {
            hostComponent.data = null;
            fixture.detectChanges();
            hostComponent.data = { restricted: true, ...mockPublishFormData };
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
                .componentInstance;
            expect(pushPublishForm.pushActions[1].disabled).toEqual(true);
            expect(pushPublishForm.pushActions[2].disabled).toEqual(true);
        });

        it('should disable remove and publish expired on cats data ', () => {
            hostComponent.data = null;
            fixture.detectChanges();
            hostComponent.data = { cats: true, ...mockPublishFormData };
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
                .componentInstance;
            expect(pushPublishForm.pushActions[1].disabled).toEqual(true);
            expect(pushPublishForm.pushActions[2].disabled).toEqual(true);
        });
    });

    it('should load custom code', () => {
        const dotParseHtmlService = fixture.debugElement.injector.get(DotParseHtmlService);
        spyOn(dotParseHtmlService, 'parse').and.callThrough();
        const mockCustomCode: DotPushPublishDialogData = {
            customCode: '<h1>Code</h1>',
            ...mockPublishFormData
        };
        hostComponent.data = null;
        fixture.detectChanges();
        hostComponent.data = mockCustomCode;
        fixture.detectChanges();
        pushPublishForm = fixture.debugElement.query(By.css('dot-push-publish-form'))
            .componentInstance;

        expect(dotParseHtmlService.parse).toHaveBeenCalledWith(
            mockCustomCode.customCode,
            pushPublishForm.customCodeContainer.nativeElement,
            true
        );
    });

    it('should be valid when environment selected', () => {
        pushPublishForm.form.get('environment').setValue(['123']);
        expect(hostComponent.valid).toEqual(true);
        expect(hostComponent.value).toEqual({ ...mockFormInitialValue, environment: ['123'] });
    });

    it('should show error messages', () => {
        selectActionButtons = fixture.debugElement.queryAll(By.css('p-selectButton .ui-button'));
        selectActionButtons[2].triggerEventHandler('click', {});
        pushPublishForm.form.get('environment').setValue(null);
        pushPublishForm.form.get('environment').markAsDirty();
        pushPublishForm.form.get('publishDate').setValue(null);
        pushPublishForm.form.get('publishDate').markAsDirty();
        pushPublishForm.form.get('expireDate').setValue(null);
        pushPublishForm.form.get('expireDate').markAsDirty();
        fixture.detectChanges();
        const errorMessages = fixture.debugElement.queryAll(By.css('.ui-messages-error'));

        expect(errorMessages[0].nativeElement.innerText).toEqual('Publish Date is required');
        expect(errorMessages[1].nativeElement.innerText).toEqual('Expire Date is required');
        expect(errorMessages[2].nativeElement.innerText).toEqual(
            'Must add at least one Environment'
        );
    });
});
