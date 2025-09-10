/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ConfirmationService, SelectItem } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { CalendarModule } from 'primeng/calendar';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { SelectButton, SelectButtonModule } from 'primeng/selectbutton';

import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPushPublishFilter,
    DotPushPublishFiltersService,
    DotRouterService,
    PushPublishService
} from '@dotcms/data-access';
import { CoreWebService, DotcmsConfigService, LoginService } from '@dotcms/dotcms-js';
import { DotPushPublishDialogData } from '@dotcms/dotcms-models';
import {
    DotDialogModule,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotcmsConfigServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService,
    mockDotTimeZones
} from '@dotcms/utils-testing';

import { DotPushPublishFormComponent } from './dot-push-publish-form.component';

import { DotParseHtmlService } from '../../../../../api/services/dot-parse-html/dot-parse-html.service';
import { PushPublishEnvSelectorComponent } from '../../dot-push-publish-env-selector/dot-push-publish-env-selector.component';
import { PushPublishServiceMock } from '../../dot-push-publish-env-selector/dot-push-publish-env-selector.component.spec';
import { PushPublishEnvSelectorModule } from '../../dot-push-publish-env-selector/dot-push-publish-env-selector.module';

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
        '@if (data) {<dot-push-publish-form (valid)="valid = $event" (value)="value = $event" [data]="data"></dot-push-publish-form>}',
    standalone: false
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

const optionsLabels: SelectItem[] = [
    { label: 'Title default', value: 'key1' },
    { label: 'Title key2', value: 'key2' },
    { label: 'A - Tittle', value: 'key3' }
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

xdescribe('DotPushPublishFormComponent', () => {
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
        environment: '',
        timezoneId: 'America/Costa Rica'
    };
    const localTZ = 'America/Costa Rica';

    beforeEach(() => {
        pushPublishServiceMock = new PushPublishServiceMock();
        TestBed.configureTestingModule({
            declarations: [DotPushPublishFormComponent, TestHostComponent],
            providers: [
                { provide: PushPublishService, useValue: pushPublishServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
                DotPushPublishFiltersService,
                DotParseHtmlService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService
            ],
            imports: [
                AutoFocusModule,
                FormsModule,
                CalendarModule,
                DotDialogModule,
                PushPublishEnvSelectorModule,
                ReactiveFormsModule,
                DropdownModule,
                DotFieldValidationMessageComponent,
                SelectButtonModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                HttpClientTestingModule
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        spyOn<any>(Intl, 'DateTimeFormat').and.returnValue({
            resolvedOptions: () => ({ timeZone: localTZ })
        });
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
        pushPublishForm = fixture.debugElement.query(
            By.css('dot-push-publish-form')
        ).componentInstance;
        pushActionsSelect = fixture.debugElement.query(By.css('p-selectButton')).componentInstance;
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it('should load filters on load', () => {
        const filterDropDown = fixture.debugElement.query(By.css('p-dropdown'));

        expect(filterDropDown.attributes['ng-reflect-autofocus']).toBe('true');
        expect(filterDropDown.componentInstance.options).toEqual(optionsLabels);
    });

    it('should set 1 previous day as a minDate for Publish Date', () => {
        const publishDateInputCalendar = fixture.debugElement.query(
            By.css('[data-testid="publishDateInputCalendar"]')
        );
        expect(publishDateInputCalendar.componentInstance._minDate).toBe(
            pushPublishForm.dateFieldMinDate
        );
    });

    it('should load timezones local label and list on load, also TimeZone dropdown must be hidden', () => {
        const timezoneDropDownContainer = fixture.debugElement.query(
            By.css('[data-testid="timeZoneSelectContainer"]')
        );
        const timezoneDropDown: Dropdown = fixture.debugElement.query(
            By.css('[data-testid="timeZoneSelect"]')
        ).componentInstance;
        const timeZoneLabel = fixture.debugElement.query(
            By.css('.push-publish-dialog__timezone-label span')
        ).nativeElement;
        expect(timezoneDropDownContainer.attributes['hidden']).toBeDefined();
        expect(timezoneDropDown.options.length).toEqual(mockDotTimeZones.length);
        expect(timeZoneLabel.outerText).toEqual(
            pushPublishForm.timeZoneOptions.find(({ value }) => value === localTZ)['label']
        );
    });

    it('should display TimeZones dropdown when "Change" link clicked', () => {
        const changeTZLink = fixture.debugElement.query(
            By.css('.push-publish-dialog__timezone-label a')
        ).nativeElement;
        changeTZLink.click();
        fixture.detectChanges();
        const timezoneDropDown: Dropdown = fixture.debugElement.query(
            By.css('[data-testid="timeZoneSelect"]')
        ).componentInstance;
        const timezoneDropDownContainer = fixture.debugElement.query(
            By.css('[data-testid="timeZoneSelectContainer"]')
        );
        expect(timezoneDropDownContainer.attributes['hidden']).not.toBeDefined();
        expect(timezoneDropDown.filter).toBe(true);
        expect(timezoneDropDown.filterBy).toBe('label');
    });

    it('should change selected TimeZone value', () => {
        const changedTZ = 'America/Panama';
        fixture.debugElement
            .query(By.css('.push-publish-dialog__timezone-label a'))
            .nativeElement.click();
        const dropdown = fixture.debugElement.query(By.css('[data-testid="timeZoneSelect"]'));
        dropdown.triggerEventHandler('onChange', {
            value: changedTZ
        });
        fixture.detectChanges();
        const timeZoneLabel = fixture.debugElement.query(
            By.css('.push-publish-dialog__timezone-label span')
        ).nativeElement;
        expect(timeZoneLabel.outerText).toEqual(
            pushPublishForm.timeZoneOptions.find(({ value }) => value === changedTZ)['label']
        );
    });

    it('should pass assetIdentifier to dot-push-publish-env-selector', () => {
        const pushPublishEnvSelectorComponent: PushPublishEnvSelectorComponent =
            fixture.debugElement.query(By.css('dot-push-publish-env-selector')).componentInstance;

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
            selectActionButtons = fixture.debugElement.queryAll(By.css('p-selectButton .p-button'));
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
            pushPublishForm = fixture.debugElement.query(
                By.css('dot-push-publish-form')
            ).componentInstance;
            expect(pushPublishForm.pushActions[2].disabled).toEqual(true);
        });

        it('should disable remove and publish expired on restricted data ', () => {
            hostComponent.data = null;
            fixture.detectChanges();
            hostComponent.data = { restricted: true, ...mockPublishFormData };
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(
                By.css('dot-push-publish-form')
            ).componentInstance;
            expect(pushPublishForm.pushActions[1].disabled).toEqual(true);
            expect(pushPublishForm.pushActions[2].disabled).toEqual(true);
        });

        it('should disable remove and publish expired on cats data ', () => {
            hostComponent.data = null;
            fixture.detectChanges();
            hostComponent.data = { cats: true, ...mockPublishFormData };
            fixture.detectChanges();
            pushPublishForm = fixture.debugElement.query(
                By.css('dot-push-publish-form')
            ).componentInstance;
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
        pushPublishForm = fixture.debugElement.query(
            By.css('dot-push-publish-form')
        ).componentInstance;

        expect(dotParseHtmlService.parse).toHaveBeenCalledWith(
            mockCustomCode.customCode,
            pushPublishForm.customCodeContainer.nativeElement,
            true
        );
    });

    it('should be valid when environment selected', () => {
        pushPublishForm.form.get('environment').setValue(['123']);
        expect(hostComponent.valid).toEqual(true);
        expect(hostComponent.value).toEqual({
            ...mockFormInitialValue,
            environment: ['123']
        });
    });

    it('should show error messages', () => {
        selectActionButtons = fixture.debugElement.queryAll(By.css('p-selectButton .p-button'));
        selectActionButtons[2].triggerEventHandler('click', {});
        pushPublishForm.form.get('environment').setValue(null);
        pushPublishForm.form.get('environment').markAsDirty();
        pushPublishForm.form.get('environment').updateValueAndValidity();

        pushPublishForm.form.get('publishDate').setValue(null);
        pushPublishForm.form.get('publishDate').markAsDirty();
        pushPublishForm.form.get('publishDate').updateValueAndValidity();

        pushPublishForm.form.get('expireDate').setValue(null);
        pushPublishForm.form.get('expireDate').markAsDirty();
        pushPublishForm.form.get('expireDate').updateValueAndValidity();

        fixture.detectChanges();
        const errorMessages = fixture.debugElement.queryAll(By.css('.p-invalid'));

        expect(errorMessages[0].nativeElement.innerText).toEqual('Publish Date is required');
        expect(errorMessages[1].nativeElement.innerText).toEqual('Expire Date is required');
        expect(errorMessages[2].nativeElement.innerText).toContain(
            'Must add at least one Environment'
        );
    });
});
