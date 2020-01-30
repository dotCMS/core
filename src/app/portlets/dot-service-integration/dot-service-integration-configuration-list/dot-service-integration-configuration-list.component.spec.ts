import { of, Observable } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotServiceIntegrationConfigurationListComponent } from './dot-service-integration-configuration-list.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { InputTextModule } from 'primeng/primeng';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotServiceIntegrationConfigurationListResolver } from './dot-service-integration-configuration-list-resolver.service';
import { By } from '@angular/platform-browser';
import { Injectable } from '@angular/core';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';

const messages = {
    'service.integration.configurations': 'Configurations',
    'service.integration.no.configurations': 'No Configurations',
    'service.integration.key': 'Key:',
    'service.integration.add.configurations': 'No configurations',
    'service.integration.no.configurations.message': 'You do not have configurations',
    'service.integration.add.configurations.button': 'Add Configuration',
    'service.integration.confirmation.delete.all.button': 'Delete All',
    'service.integration.confirmation.title': 'Are you sure?',
    'service.integration.confirmation.delete.message': 'Delete this?',
    'service.integration.confirmation.delete.all.message': 'Delete all?',
    'service.integration.confirmation.accept': 'Ok'
};

const serviceData = {
    configurationsCount: 2,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: "It's a tool to keep track of your life's events",
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites: [
        {
            id: '123',
            name: 'demo.dotcms.com'
        },
        {
            id: '456',
            name: 'host.example.com'
        }
    ]
};

const routeDatamock = {
    data: [serviceData, messages]
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }
}

@Injectable()
class MockDotServiceIntegrationService {
    deleteConfiguration(_configurationId: string): Observable<string> {
        return of('');
    }
    deleteAllConfigurations(): Observable<string> {
        return of('');
    }
}

describe('DotServiceIntegrationConfigurationListComponent', () => {
    let component: DotServiceIntegrationConfigurationListComponent;
    let fixture: ComponentFixture<DotServiceIntegrationConfigurationListComponent>;
    let dialogService: DotAlertConfirmService;
    let integrationService: DotServiceIntegrationService;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotServiceIntegrationConfigurationListComponent,
                        path: ''
                    }
                ]),
                DotAvatarModule,
                DotCopyButtonModule,
                DotIconButtonModule,
                DotActionButtonModule,
                InputTextModule
            ],
            declarations: [DotServiceIntegrationConfigurationListComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotServiceIntegrationService,
                    useClass: MockDotServiceIntegrationService
                },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },
                DotServiceIntegrationConfigurationListResolver
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotServiceIntegrationConfigurationListComponent);
        component = fixture.debugElement.componentInstance;
        dialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
        integrationService = fixture.debugElement.injector.get(DotServiceIntegrationService);
        routerService = fixture.debugElement.injector.get(DotRouterService);
    });

    describe('With integrations count', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should set Service Integration from resolver', () => {
            expect(component.serviceIntegration).toBe(serviceData);
            expect(component.messagesKey).toBe(messages);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__service-name')
                ).nativeElement.innerText
            ).toBe(component.serviceIntegration.name);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__service-key')
                ).nativeElement.textContent
            ).toContain(
                `${component.messagesKey['service.integration.key']} ${component.serviceIntegration.key}`
            );

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__configurations')
                ).nativeElement.textContent
            ).toContain(
                `${component.serviceIntegration.configurationsCount} ${component.messagesKey['service.integration.configurations']}`
            );

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__description')
                ).nativeElement.innerText
            ).toBe(component.serviceIntegration.description);

            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-service-integration-configuration__name')
                )[0].nativeElement.innerText
            ).toBe(component.serviceIntegration.sites[0].name);

            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-service-integration-configuration__host-key')
                )[0].nativeElement.innerText
            ).toBe(
                `${component.messagesKey['service.integration.key']} ${component.serviceIntegration.sites[0].id}`
            );
        });

        it('should show 2 host configurations', () => {
            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-service-integration-configuration__item')
                ).length
            ).toBe(2);
        });

        it('should have Dot-Copy-Button with serviceKey value', () => {
            const copyBtn = fixture.debugElement.query(By.css('dot-copy-button')).componentInstance;
            expect(copyBtn.copy).toBe(component.serviceIntegration.key);
            expect(copyBtn.label).toBe(component.serviceIntegration.key);
        });

        it('should have Dot-Avatar with correct values', () => {
            const avatar = fixture.debugElement.query(By.css('dot-avatar')).componentInstance;
            expect(avatar.size).toBe(112);
            expect(avatar.url).toBe(component.serviceIntegration.iconUrl);
        });

        it('should redirect to new configuration page', () => {
            const addBtn = fixture.debugElement.query(By.css('dot-action-button')).nativeElement;
            addBtn.click();
            expect(routerService.gotoPortlet).toHaveBeenCalledWith(
                `/integration-services/${component.serviceIntegration.key}/new`
            );
        });

        it('should redirect to edit configuration page on Edit Icon button', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const editBtn = fixture.debugElement.queryAll(
                By.css('.dot-service-integration-configuration__item dot-icon-button')
            )[1];
            editBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy
            });
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(routerService.gotoPortlet).toHaveBeenCalledWith(
                `/integration-services/${component.serviceIntegration.key}/edit/${serviceData.sites[0].id}`
            );
        });

        it('should redirect to edit configuration page on item element click', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const editBtn = fixture.debugElement.queryAll(
                By.css('.dot-service-integration-configuration__item')
            )[0];
            editBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy
            });
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(routerService.gotoPortlet).toHaveBeenCalledWith(
                `/integration-services/${component.serviceIntegration.key}/edit/${serviceData.sites[0].id}`
            );
        });

        it('should open confirm dialog and delete All configurations', () => {
            const deleteAllBtn = fixture.debugElement.query(
                By.css('.dot-service-integration-configuration__delete_all button')
            );

            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            spyOn(integrationService, 'deleteAllConfigurations');

            deleteAllBtn.triggerEventHandler('click', null);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(integrationService.deleteAllConfigurations).toHaveBeenCalledWith(
                component.serviceIntegration.key
            );
        });

        it('should open confirm dialog and delete configuration', () => {
            const stopPropagationSpy = jasmine.createSpy('spy');
            const deleteBtn = fixture.debugElement.queryAll(
                By.css('.dot-service-integration-configuration__item dot-icon-button')
            )[0];

            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            spyOn(integrationService, 'deleteConfiguration');

            deleteBtn.triggerEventHandler('click', {
                stopPropagation: stopPropagationSpy
            });
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
            expect(integrationService.deleteConfiguration).toHaveBeenCalledWith(
                component.serviceIntegration.key,
                serviceData.sites[0].id
            );
        });
    });

    describe('With NO integrations count', () => {
        beforeEach(() => {
            routeDatamock.data[0] = {
                ...serviceData,
                configurationsCount: 0,
                sites: []
            };
            fixture.detectChanges();
        });

        it('should show No configurations label', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__configurations')
                ).nativeElement.textContent
            ).toContain(component.messagesKey['service.integration.no.configurations']);
        });

        it('should show no configurations container', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__add-configurations')
                )
            ).toBeTruthy();
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__add-configurations-title')
                ).nativeElement.innerText
            ).toBe(component.messagesKey['service.integration.add.configurations']);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__add-configurations-description')
                ).nativeElement.innerText
            ).toBe(component.messagesKey['service.integration.no.configurations.message']);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-service-integration-configuration__add-configurations button')
                ).nativeElement.textContent
            ).toBe(component.messagesKey['service.integration.add.configurations.button']);
        });

        it('should redirect to new configuration page', () => {
            const addBtn = fixture.debugElement.query(
                By.css('.dot-service-integration-configuration__add-configurations button')
            ).nativeElement;
            addBtn.click();
            expect(routerService.gotoPortlet).toHaveBeenCalledWith(
                `/integration-services/${component.serviceIntegration.key}/new`
            );
        });
    });
});
