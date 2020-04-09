import { of } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { ActivatedRoute } from '@angular/router';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotAppsListComponent } from './dot-apps-list.component';
import { InputTextModule } from 'primeng/primeng';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotAppsCardModule } from './dot-apps-card/dot-apps-card.module';
import { By } from '@angular/platform-browser';
import { DotAppsCardComponent } from './dot-apps-card/dot-apps-card.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';

class AppsServicesMock {
    get() {}
}

const routeDatamock = {
    appsServices: [
        {
            configurationsCount: 0,
            key: 'google-calendar',
            name: 'Google Calendar',
            description: "It's a tool to keep track of your life's events",
            iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg'
        },
        {
            configurationsCount: 1,
            key: 'asana',
            name: 'Asana',
            description: "It's asana to keep track of your asana events",
            iconUrl: '/dA/792c7c9f-6b6f-427b-80ff-1643376c9999/photo/mountain-persona.jpg'
        }
    ]
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }
}

describe('DotAppsListComponent', () => {
    let component: DotAppsListComponent;
    let fixture: ComponentFixture<DotAppsListComponent>;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService({
        'apps.search.placeholder': 'Search'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotAppsListComponent,
                        path: ''
                    }
                ]),
                DotAppsCardModule,
                InputTextModule
            ],
            declarations: [DotAppsListComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },
                { provide: DotAppsService, useClass: AppsServicesMock },
                DotAppsListResolver
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsListComponent);
        component = fixture.debugElement.componentInstance;
        routerService = fixture.debugElement.injector.get(DotRouterService);
        fixture.detectChanges();
    });

    it('should set App from resolver', () => {
        expect(component.apps).toBe(routeDatamock.appsServices);
        expect(component.appsCopy).toEqual(routeDatamock.appsServices);
    });

    it('should contain 2 app configurations', () => {
        expect(fixture.debugElement.queryAll(By.css('dot-apps-card')).length).toBe(2);
    });

    it('should set messages to Search Input', () => {
        expect(fixture.debugElement.query(By.css('input')).nativeElement.placeholder).toBe(
            component.messagesKey['apps.search.placeholder']
        );
    });

    it('should set app data to service Card', () => {
        expect(
            fixture.debugElement.queryAll(By.css('dot-apps-card'))[0].componentInstance.app
        ).toEqual(routeDatamock.appsServices[0]);
    });

    it('should redirect to detail configuration list page when app Card clicked', () => {
        const card: DotAppsCardComponent = fixture.debugElement.queryAll(By.css('dot-apps-card'))[0]
            .componentInstance;
        card.actionFired.emit(component.apps[0].key);
        expect(routerService.gotoPortlet).toHaveBeenCalledWith(`/apps/${component.apps[0].key}`);
    });
});
