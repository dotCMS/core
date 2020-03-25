import { of, Observable } from 'rxjs';
import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { InputTextModule, ButtonModule } from 'primeng/primeng';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { By } from '@angular/platform-browser';
import { Injectable } from '@angular/core';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { CommonModule } from '@angular/common';
import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { PaginatorService } from '@services/paginator';

const messages = {
    'apps.key': 'Key',
    'apps.configurations': 'Configurations',
    'apps.no.configurations': 'No Configurations',
    'apps.confirmation.delete.all.button': 'Delete All',
    'apps.confirmation.title': 'Are you sure?',
    'apps.confirmation.description.show.more': 'Show More',
    'apps.confirmation.description.show.less': 'Show Less',
    'apps.confirmation.delete.all.message': 'Delete all?',
    'apps.confirmation.accept': 'Ok',
    'apps.search.placeholder': 'Search by name'
};

const sites = [
    {
        configured: true,
        id: '123',
        name: 'demo.dotcms.com'
    },
    {
        configured: false,
        id: '456',
        name: 'host.example.com'
    }
];

const serviceData = {
    configurationsCount: 2,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: `It is a tool to keep track of your life\'s events`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites
};

const routeDatamock = {
    data: { service: serviceData, messages }
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }
}

@Injectable()
class MockDotAppsService {
    deleteConfiguration(_configurationId: string): Observable<string> {
        return of('');
    }
    deleteAllConfigurations(): Observable<string> {
        return of('');
    }
}

describe('DotAppsConfigurationComponent', () => {
    let component: DotAppsConfigurationComponent;
    let fixture: ComponentFixture<DotAppsConfigurationComponent>;
    let dialogService: DotAlertConfirmService;
    let paginationService: PaginatorService;
    let appsServices: DotAppsService;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotAppsConfigurationComponent,
                        path: ''
                    }
                ]),
                InputTextModule,
                ButtonModule,
                CommonModule,
                DotAvatarModule,
                DotActionButtonModule,
                DotCopyButtonModule,
                DotAppsConfigurationListModule
            ],
            declarations: [DotAppsConfigurationComponent],
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
                DotAppsConfigurationResolver,
                PaginatorService
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationComponent);
        component = fixture.debugElement.componentInstance;
        dialogService = fixture.debugElement.injector.get(DotAlertConfirmService);
        paginationService = fixture.debugElement.injector.get(PaginatorService);
        appsServices = fixture.debugElement.injector.get(DotAppsService);
        routerService = fixture.debugElement.injector.get(DotRouterService);
    });

    describe('With integrations count', () => {
        beforeEach(() => {
            spyOn(paginationService, 'setExtraParams');
            spyOn(paginationService, 'getWithOffset').and.returnValue(of(serviceData));
            spyOn(component.searchInput.nativeElement, 'focus');
            fixture.detectChanges();
        });

        it('should set Service Integration from resolver', () => {
            expect(component.serviceIntegration).toBe(serviceData);
            expect(component.messagesKey).toBe(messages);
        });

        it('should set onInit Pagination Service with right values', () => {
            expect(paginationService.url).toBe(
                `v1/apps/${component.serviceIntegration.key}`
            );
            expect(paginationService.paginationPerPage).toBe(component.paginationPerPage);
            expect(paginationService.sortField).toBe('name');
            expect(paginationService.sortOrder).toBe(1);
            expect(paginationService.setExtraParams).toHaveBeenCalledWith('filter', '');
        });

        it('should call first pagination call onInit', () => {
            expect(paginationService.getWithOffset).toHaveBeenCalledWith(0);
        });

        it('should input search be focused on init', () => {
            expect(component.searchInput.nativeElement.focus).toHaveBeenCalledTimes(1);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__service-name')
                ).nativeElement.innerText
            ).toBe(component.serviceIntegration.name);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__service-key')
                ).nativeElement.textContent
            ).toContain(
                `${component.messagesKey['apps.key']} ${component.serviceIntegration.key}`
            );

            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__configurations')
                ).nativeElement.textContent
            ).toContain(
                `${component.serviceIntegration.configurationsCount} ${component.messagesKey['apps.configurations']}`
            );

            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__description')
                ).nativeElement.innerText
            ).toContain(`${component.serviceIntegration.description}`);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__action_header input')
                ).nativeElement.placeholder
            ).toContain(component.messagesKey['apps.search.placeholder']);

            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__action_header button')
                ).nativeElement.innerText
            ).toContain(
                component.messagesKey[
                    'apps.confirmation.delete.all.button'
                ].toUpperCase()
            );
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

        it('should have dot-apps-configuration-list with correct values', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            expect(listComp.siteConfigurations).toBe(component.serviceIntegration.sites);
            expect(listComp.disabledLoadDataButton).toBe(true);
            expect(listComp.itemsPerPage).toBe(component.paginationPerPage);
        });

        it('should dot-apps-configuration-list emit action to load more data', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.loadData.emit({ first: 10 });
            expect(paginationService.getWithOffset).toHaveBeenCalledWith(10);
        });

        it('should redirect to goto configuration page action', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.edit.emit(sites[0]);
            expect(routerService.goToAppsServices).toHaveBeenCalledWith(
                component.serviceIntegration.key,
                sites[0]
            );
        });

        it('should open confirm dialog and delete All configurations', () => {
            const deleteAllBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_header button')
            );

            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            spyOn(appsServices, 'deleteAllConfigurations').and.returnValue(of(null));

            deleteAllBtn.triggerEventHandler('click', null);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(appsServices.deleteAllConfigurations).toHaveBeenCalledWith(
                component.serviceIntegration.key
            );
        });

        it('should delete a specific configuration', () => {
            spyOn(appsServices, 'deleteConfiguration').and.returnValue(of(null));
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.delete.emit(sites[0]);

            expect(appsServices.deleteConfiguration).toHaveBeenCalledWith(
                component.serviceIntegration.key,
                sites[0].id
            );
        });

        it('should call service integration filter on search', fakeAsync(() => {
            component.searchInput.nativeElement.value = 'test';
            component.searchInput.nativeElement.dispatchEvent(new Event('keyup'));
            tick(550);
            expect(paginationService.setExtraParams).toHaveBeenCalledWith('filter', 'test');
            expect(paginationService.getWithOffset).toHaveBeenCalled();
        }));
    });

    describe('With NO integrations count', () => {
        beforeEach(() => {
            routeDatamock.data.service = {
                ...serviceData,
                configurationsCount: 0
            };
            fixture.detectChanges();
        });

        it('should show No configurations label', () => {
            expect(
                fixture.debugElement.query(
                    By.css('.dot-apps-configuration__configurations')
                ).nativeElement.textContent
            ).toContain(component.messagesKey['apps.no.configurations']);
        });
    });
});
