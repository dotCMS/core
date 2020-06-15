import { of, Observable } from 'rxjs';
import * as _ from 'lodash';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { Injectable, Component, Input, Output, EventEmitter } from '@angular/core';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationDetailFormModule } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.module';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { ButtonModule } from 'primeng/button';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';
import { By } from '@angular/platform-browser';
import { DotAppsSaveData } from '@shared/models/dot-apps/dot-apps.model';
import { DotKeyValue } from '@shared/models/dot-key-value/dot-key-value.model';
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
    description: `It is a tool to keep track of your life\'s events`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites
};

const routeDatamock = {
    data: appData
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
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
    selector: 'dot-key-value',
    template: ''
})
class MockDotKeyValueComponent {
    @Input() autoFocus: boolean;
    @Input() showHiddenField: string;
    @Input() variables: DotKeyValue[];
    @Output() delete = new EventEmitter<DotKeyValue>();
    @Output() save = new EventEmitter<DotKeyValue>();
}

describe('DotAppsConfigurationDetailComponent', () => {
    let component: DotAppsConfigurationDetailComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailComponent>;
    let appsServices: DotAppsService;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
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
                    DotAppsConfigurationDetailFormModule
                ],
                declarations: [DotAppsConfigurationDetailComponent, MockDotKeyValueComponent],
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
        })
    );

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationDetailComponent);
        component = fixture.debugElement.componentInstance;
        appsServices = fixture.debugElement.injector.get(DotAppsService);
        routerService = fixture.debugElement.injector.get(DotRouterService);
        spyOn(appsServices, 'saveSiteConfiguration').and.returnValue(of({}));
    });

    describe('Without dynamic params', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should set App from resolver', () => {
            expect(component.apps).toBe(appData);
        });

        it('should set labels and buttons with right values', () => {
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail-actions button')
                )[0].nativeElement.innerText
            ).toContain(messageServiceMock.get('Cancel'));
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail-actions button')
                )[1].nativeElement.innerText
            ).toContain(messageServiceMock.get('Save'));
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-detail__host-name'))
                    .nativeElement.textContent
            ).toContain(component.apps.sites[0].name);
            expect(fixture.debugElement.query(By.css('dot-key-value'))).toBeFalsy();
        });

        it('should set Save button disabled to false with valid form', () => {
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail-actions button')
                )[1].nativeElement.disabled
            ).toBe(false);
        });

        it('should set form component with fields data', () => {
            const formComponent = fixture.debugElement.query(
                By.css('dot-apps-configuration-detail-form')
            ).componentInstance;
            expect(formComponent.formFields).toEqual(sites[0].secrets);
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
            const cancelBtn = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail-actions button')
            )[0];
            cancelBtn.triggerEventHandler('click', {});
            expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.apps.key);
        });

        it('should have Dot-Copy-Button with appKey value', () => {
            const copyBtn = fixture.debugElement.query(By.css('dot-copy-button')).componentInstance;
            expect(copyBtn.copy).toBe(component.apps.key);
            expect(copyBtn.label).toBe(component.apps.key);
        });

        it('should save configuration when Save button clicked', () => {
            const transformedData = {
                name: {
                    hidden: false,
                    value: 'John'
                },
                password: {
                    hidden: false,
                    value: '****'
                },
                enabled: {
                    hidden: false,
                    value: 'true'
                }
            };
            const saveBtn = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail-actions button')
            )[1];

            saveBtn.triggerEventHandler('click', {});
            expect(appsServices.saveSiteConfiguration).toHaveBeenCalledWith(
                component.apps.key,
                component.apps.sites[0].id,
                transformedData
            );
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
            routeDatamock.data = {
                ...appData,
                allowExtraParams: true,
                sites: sitesDynamic
            };
            fixture.detectChanges();
        });

        it('should show DotKeyValue component with right values', () => {
            const keyValue = fixture.debugElement.query(By.css('dot-key-value'));
            expect(keyValue).toBeTruthy();
            expect(keyValue.componentInstance.autoFocus).toBe(false);
            expect(keyValue.componentInstance.showHiddenField).toBe(true);
            expect(keyValue.componentInstance.variables).toEqual([
                { key: 'custom', hidden: false, value: 'test' }
            ]);
        });

        it('should update local collection with saved value', () => {
            const variableEmitted = { key: 'custom', hidden: false, value: 'changed' };
            const keyValue = fixture.debugElement.query(By.css('dot-key-value'));
            keyValue.componentInstance.save.emit(variableEmitted);
            expect(component.dynamicVariables[0]).toEqual(variableEmitted);
            expect(component.dynamicVariables.length).toEqual(1);
        });

        it('should delete from local collection', () => {
            const variableEmitted = { key: 'custom', hidden: false, value: 'test' };
            const keyValue = fixture.debugElement.query(By.css('dot-key-value'));
            keyValue.componentInstance.delete.emit(variableEmitted);
            expect(component.dynamicVariables.length).toEqual(0);
        });

        it('should save configuration when Save button clicked', () => {
            const transformedData = {
                name: {
                    hidden: false,
                    value: 'John'
                },
                password: {
                    hidden: false,
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
            const saveBtn = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration-detail-actions button')
            )[1];

            saveBtn.triggerEventHandler('click', {});
            expect(appsServices.saveSiteConfiguration).toHaveBeenCalledWith(
                component.apps.key,
                component.apps.sites[0].id,
                transformedData
            );
        });
    });
});
