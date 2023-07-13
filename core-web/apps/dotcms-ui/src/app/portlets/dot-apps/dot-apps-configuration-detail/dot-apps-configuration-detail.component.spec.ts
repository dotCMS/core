import * as _ from 'lodash';
import { MarkdownModule } from 'ngx-markdown';
import { Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Injectable, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';

import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsService } from '@dotcms/app/api/services/dot-apps/dot-apps.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessageService } from '@dotcms/data-access';
import { DotAppsSaveData, DotAppsSecrets } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, MockDotRouterService } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotKeyValue } from '@shared/models/dot-key-value-ng/dot-key-value-ng.model';

import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';

import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';

const messages = {
    'apps.key': 'Key',
    Cancel: 'CANCEL',
    Save: 'SAVE',
    'apps.custom.properties': 'Custom Properties',
    'apps.form.dialog.success.header': 'Header',
    'apps.form.dialog.success.message': 'Message',
    ok: 'OK'
};

const sites = [
    {
        configured: false,
        id: '456',
        name: 'host.example.com',
        secrets: [
            {
                dynamic: false,
                name: 'name',
                hidden: false,
                hint: 'Hint for Name',
                label: 'Name:',
                required: true,
                type: 'STRING',
                value: 'John'
            },
            {
                dynamic: false,
                name: 'password',
                hidden: true,
                hint: 'Hint for Password',
                label: 'Password:',
                required: true,
                type: 'STRING',
                value: '****'
            },
            {
                dynamic: false,
                name: 'enabled',
                hidden: false,
                hint: 'Hint for checkbox',
                label: 'Enabled:',
                required: false,
                type: 'BOOL',
                value: 'true'
            }
        ]
    }
];

const appData = {
    allowExtraParams: false,
    configurationsCount: 1,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: `It is a tool to keep track of your life's events`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites
};

const routeDatamock = {
    data: appData
};

class ActivatedRouteMock {
    get data() {
        return {};
    }
}

@Injectable()
class MockDotAppsService {
    saveSiteConfiguration(
        _appKey: string,
        _id: string,
        _params: DotAppsSaveData
    ): Observable<string> {
        return of('');
    }
}

@Component({
    selector: 'dot-key-value-ng',
    template: ''
})
class MockDotKeyValueComponent {
    @Input() autoFocus: boolean;
    @Input() showHiddenField: string;
    @Input() variables: DotKeyValue[];
    @Output() delete = new EventEmitter<DotKeyValue>();
    @Output() save = new EventEmitter<DotKeyValue>();
}

@Component({
    selector: 'dot-apps-configuration-detail-form',
    template: ''
})
class MockDotAppsConfigurationDetailFormComponent {
    @Input() appConfigured: boolean;
    @Input() formFields: DotAppsSecrets[];
    @Output() data = new EventEmitter<{ [key: string]: string }>();
    @Output() valid = new EventEmitter<boolean>();
}

describe('DotAppsConfigurationDetailComponent', () => {
    let component: DotAppsConfigurationDetailComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailComponent>;
    let appsServices: DotAppsService;
    let activatedRoute: ActivatedRoute;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotAppsConfigurationDetailComponent,
                        path: ''
                    }
                ]),
                ButtonModule,
                CommonModule,
                DotCopyButtonModule,
                DotAppsConfigurationHeaderModule,
                DotPipesModule,
                DotMessagePipe,
                MarkdownModule.forRoot()
            ],
            declarations: [
                DotAppsConfigurationDetailComponent,
                MockDotKeyValueComponent,
                MockDotAppsConfigurationDetailFormComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotAppsService,
                    useClass: MockDotAppsService
                },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },

                DotAppsConfigurationDetailResolver
            ]
        });

        fixture = TestBed.createComponent(DotAppsConfigurationDetailComponent);
        component = fixture.debugElement.componentInstance;
        appsServices = TestBed.inject(DotAppsService);
        routerService = TestBed.inject(DotRouterService);
        activatedRoute = TestBed.inject(ActivatedRoute);
        spyOn(appsServices, 'saveSiteConfiguration').and.callThrough();
    }));

    describe('Without dynamic params', () => {
        beforeEach(() => {
            spyOnProperty(activatedRoute, 'data').and.returnValue(of(routeDatamock));
            fixture.detectChanges();
        });

        it('should set App from resolver', () => {
            expect(component.apps).toBe(appData);
        });

        it('should set labels and buttons with right values', () => {
            expect(
                fixture.debugElement.query(By.css('[data-testid="cancelBtn"]')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('Cancel'));
            expect(
                fixture.debugElement.query(By.css('[data-testid="saveBtn"]')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('Save'));
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-detail__host-name'))
                    .nativeElement.textContent
            ).toContain(component.apps.sites[0].name);
            expect(fixture.debugElement.query(By.css('dot-key-value-ng'))).toBeFalsy();
        });

        it('should set Save button disabled to false with valid form', () => {
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            formComponent.valid.emit(true);
            fixture.detectChanges();
            expect(
                fixture.debugElement.query(By.css('[data-testid="saveBtn"]')).nativeElement.disabled
            ).toBe(false);
        });

        it('should set form component with fields & app configured data', () => {
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            expect(formComponent.formFields).toEqual(sites[0].secrets);
            expect(formComponent.appConfigured).toEqual(sites[0].configured);
        });

        it('should update formData and formValid fields when dot-apps-configuration-detail-form changed', () => {
            const emittedData = {
                name: 'Test',
                password: 'Changed',
                enabled: 'true'
            };
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            formComponent.data.emit(emittedData);
            formComponent.valid.emit(false);
            expect(component.formData).toBe(emittedData);
            expect(component.formValid).toBe(false);
        });

        it('should redirect to Apps page when Cancel button clicked', () => {
            const cancelBtn = fixture.debugElement.query(By.css('[data-testid="cancelBtn"]'));
            cancelBtn.triggerEventHandler('click', {});
            expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.apps.key);
        });

        it('should have dot-copy-link with appKey value', () => {
            const copyBtn = fixture.debugElement.query(By.css('dot-copy-link')).componentInstance;
            expect(copyBtn.copy).toBe(component.apps.key);
            expect(copyBtn.label).toBe(component.apps.key);
        });

        it('should save configuration when Save button clicked', async () => {
            const transformedData: DotAppsSaveData = {
                name: {
                    hidden: false,
                    value: 'John'
                },
                password: {
                    hidden: true,
                    value: '****'
                },
                enabled: {
                    hidden: false,
                    value: 'true'
                }
            };
            const emittedData = {
                name: 'John',
                password: '****',
                enabled: 'true'
            };
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            formComponent.data.emit(emittedData);

            fixture.detectChanges();

            const saveBtn = fixture.debugElement.query(By.css('[data-testid="saveBtn"]'));
            saveBtn.triggerEventHandler('click', {});

            expect<(appKey: string, id: string, params: DotAppsSaveData) => Observable<string>>(
                appsServices.saveSiteConfiguration
            ).toHaveBeenCalledWith(component.apps.key, component.apps.sites[0].id, transformedData);
        });
    });

    describe('With dynamic variables', () => {
        beforeEach(() => {
            const sitesDynamic = _.cloneDeep(sites);
            sitesDynamic[0].secrets = [
                ...sites[0].secrets,
                {
                    dynamic: true,
                    name: 'custom',
                    hidden: false,
                    hint: 'dynamic variable',
                    label: '',
                    required: false,
                    type: 'STRING',
                    value: 'test'
                }
            ];
            const mockRoute = { data: {} };
            mockRoute.data = {
                ...appData,
                allowExtraParams: true,
                sites: sitesDynamic
            };
            spyOnProperty(activatedRoute, 'data').and.returnValue(of(mockRoute));

            fixture.detectChanges();
        });

        it('should show DotKeyValue component with right values', () => {
            const keyValue = fixture.debugElement.query(By.css('dot-key-value-ng'));
            expect(keyValue).toBeTruthy();
            expect(keyValue.componentInstance.autoFocus).toBe(false);
            expect(keyValue.componentInstance.showHiddenField).toBe(true);
            expect(keyValue.componentInstance.variables).toEqual([
                { key: 'custom', hidden: false, value: 'test' }
            ]);
        });

        it('should update local collection with saved value', () => {
            const variableEmitted = { key: 'custom', hidden: false, value: 'changed' };
            const keyValue = fixture.debugElement.query(By.css('dot-key-value-ng'));
            keyValue.componentInstance.save.emit(variableEmitted);
            expect(component.dynamicVariables[0]).toEqual(variableEmitted);
            expect(component.dynamicVariables.length).toEqual(1);
        });

        it('should delete from local collection', () => {
            const variableEmitted = { key: 'custom', hidden: false, value: 'test' };
            const keyValue = fixture.debugElement.query(By.css('dot-key-value-ng'));
            keyValue.componentInstance.delete.emit(variableEmitted);
            expect(component.dynamicVariables.length).toEqual(0);
        });

        it('should save configuration when Save button clicked', () => {
            const transformedData: DotAppsSaveData = {
                name: {
                    hidden: false,
                    value: 'John'
                },
                password: {
                    hidden: true,
                    value: '****'
                },
                enabled: {
                    hidden: false,
                    value: 'true'
                },
                custom: {
                    hidden: false,
                    value: 'test'
                }
            };
            const emittedData = {
                name: 'John',
                password: '****',
                enabled: 'true'
            };
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            formComponent.data.emit(emittedData);

            const saveBtn = fixture.debugElement.query(By.css('[data-testid="saveBtn"]'));

            saveBtn.triggerEventHandler('click', {});
            expect(appsServices.saveSiteConfiguration).toHaveBeenCalledWith(
                component.apps.key,
                component.apps.sites[0].id,
                transformedData
            );
        });
    });
});
