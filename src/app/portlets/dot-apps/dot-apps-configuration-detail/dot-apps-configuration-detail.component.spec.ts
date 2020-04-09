import { of, Observable } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { Injectable } from '@angular/core';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationDetailFormModule } from './dot-apps-configuration-detail-form/dot-apps-configuration-detail-form.module';
import { DotAppsConfigurationDetailResolver } from './dot-apps-configuration-detail-resolver.service';
import { ButtonModule } from 'primeng/button';
import { DotAppsConfigurationDetailComponent } from './dot-apps-configuration-detail.component';
import { By } from '@angular/platform-browser';
import { DotAppsSaveData } from '@shared/models/dot-apps/dot-apps.model';

const messages = {
    'apps.key': 'Key',
    Cancel: 'CANCEL',
    Save: 'SAVE',
    'apps.add.property': 'ADD PROPERTY',
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
    configurationsCount: 1,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: `It is a tool to keep track of your life\'s events`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites
};

const routeDatamock = {
    data: { app: appData, messages }
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

describe('DotAppsConfigurationDetailComponent', () => {
    let component: DotAppsConfigurationDetailComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailComponent>;
    let appsServices: DotAppsService;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
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
                DotAppsConfigurationDetailFormModule
            ],
            declarations: [DotAppsConfigurationDetailComponent],
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
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationDetailComponent);
        component = fixture.debugElement.componentInstance;
        appsServices = fixture.debugElement.injector.get(DotAppsService);
        routerService = fixture.debugElement.injector.get(DotRouterService);
    });

    describe('With integrations count', () => {
        beforeEach(() => {
            spyOn(appsServices, 'saveSiteConfiguration').and.returnValue(of({}));
            fixture.detectChanges();
        });

        it('should set App & Messages from resolver', () => {
            expect(component.apps).toBe(appData);
            expect(component.messagesKey).toBe(messages);
        });

        it('should set labels and buttons with right values', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-detail__service-name'))
                    .nativeElement.textContent
            ).toContain(component.apps.name);
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-detail__service-key'))
                    .nativeElement.textContent
            ).toContain(`${component.messagesKey['apps.key']} ${component.apps.key}`);
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail-actions button')
                )[0].nativeElement.innerText
            ).toContain(component.messagesKey['Cancel']);
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail-actions button')
                )[1].nativeElement.innerText
            ).toContain(component.messagesKey['Save']);
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration-detail__host-name'))
                    .nativeElement.textContent
            ).toContain(component.apps.sites[0].name);
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration-detail__form-content button')
                )[0].nativeElement.innerText
            ).toContain(component.messagesKey['apps.add.property']);
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
            expect(formComponent.formFields).toBe(sites[0].secrets);
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
            expect(routerService.gotoPortlet).toHaveBeenCalledWith(`/apps/${component.apps.key}`);
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
});
