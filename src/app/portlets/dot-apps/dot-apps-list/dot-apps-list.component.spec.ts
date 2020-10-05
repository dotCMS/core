import { of } from 'rxjs';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ActivatedRoute } from '@angular/router';
import { DotAppsListComponent } from './dot-apps-list.component';
import { InputTextModule } from 'primeng/primeng';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotAppsCardModule } from './dot-apps-card/dot-apps-card.module';
import { By } from '@angular/platform-browser';
import { DotAppsCardComponent } from './dot-apps-card/dot-apps-card.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { NotLicensedModule } from '@components/not-licensed/not-licensed.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

export class AppsServicesMock {
    get() {}
}

export const appsResponse = [
    {
        allowExtraParams: true,
        configurationsCount: 0,
        key: 'google-calendar',
        name: 'Google Calendar',
        description: "It's a tool to keep track of your life's events",
        iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
    },
    {
        allowExtraParams: true,
        configurationsCount: 1,
        key: 'asana',
        name: 'Asana',
        description: "It's asana to keep track of your asana events",
        iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
    }
];

let canAccessPortletResponse = {
    dotAppsListResolverData: {
        apps: appsResponse,
        isEnterpriseLicense: true
    }
};

class ActivatedRouteMock {
    get data() {
        return of(canAccessPortletResponse);
    }
}

describe('DotAppsListComponent', () => {
    let component: DotAppsListComponent;
    let fixture: ComponentFixture<DotAppsListComponent>;
    let routerService: DotRouterService;
    let dotAppsService: DotAppsService;

    const messageServiceMock = new MockDotMessageService({
        'apps.search.placeholder': 'Search'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotAppsListComponent,
                        path: ''
                    }
                ]),
                DotAppsCardModule,
                InputTextModule,
                NotLicensedModule,
                DotPipesModule
            ],
            declarations: [DotAppsListComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },
                { provide: DotAppsService, useClass: AppsServicesMock },
                DotAppsListResolver,
                DotLicenseService
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAppsListComponent);
        component = fixture.debugElement.componentInstance;
        routerService = TestBed.get(DotRouterService);
        dotAppsService = TestBed.get(DotAppsService);
    });

    describe('With access to portlet', () => {
        beforeEach(() => {
            spyOn(dotAppsService, 'get').and.returnValue(of(appsResponse));
            fixture.detectChanges();
        });

        it('should set App from resolver', () => {
            expect(component.apps).toBe(appsResponse);
            expect(component.appsCopy).toEqual(appsResponse);
        });

        it('should contain 2 app configurations', () => {
            expect(fixture.debugElement.queryAll(By.css('dot-apps-card')).length).toBe(2);
        });

        it('should set messages to Search Input', () => {
            expect(fixture.debugElement.query(By.css('input')).nativeElement.placeholder).toBe(
                messageServiceMock.get('apps.search.placeholder')
            );
        });

        it('should set app data to service Card', () => {
            expect(
                fixture.debugElement.queryAll(By.css('dot-apps-card'))[0].componentInstance.app
            ).toEqual(appsResponse[0]);
        });

        it('should redirect to detail configuration list page when app Card clicked', () => {
            const card: DotAppsCardComponent = fixture.debugElement.queryAll(
                By.css('dot-apps-card')
            )[0].componentInstance;
            card.actionFired.emit(component.apps[0].key);
            expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.apps[0].key);
        });
    });

    describe('Without access to portlet', () => {
        beforeEach(() => {
            canAccessPortletResponse = {
                dotAppsListResolverData: {
                    apps: null,
                    isEnterpriseLicense: false
                }
            };
            fixture.detectChanges();
        });

        it('should display not licensed component', () => {
            expect(fixture.debugElement.query(By.css('dot-not-licensed-component'))).toBeTruthy();
        });
    });
});
