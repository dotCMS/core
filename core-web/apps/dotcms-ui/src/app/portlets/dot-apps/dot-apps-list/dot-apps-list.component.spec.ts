import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { of } from 'rxjs';

import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotAppsListComponent } from './dot-apps-list.component';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { MockDotNotLicensedComponent } from '@tests/dot-not-licensed.component.mock';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import { ButtonModule } from 'primeng/button';

export class AppsServicesMock {
    get() {
        return of({});
    }
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

@Component({
    selector: 'dot-apps-card',
    template: ''
})
class MockDotAppsCardComponent {
    @Input() app: DotApps;
    @Output() actionFired = new EventEmitter<string>();
}

@Component({
    selector: 'dot-icon',
    template: ''
})
class MockDotIconComponent {
    @Input() name: string;
}

@Component({
    selector: 'dot-apps-import-export-dialog',
    template: ''
})
class MockDotAppsImportExportDialogComponent {
    @Input() action: string;
    @Input() show: boolean;
    @Output() resolved = new EventEmitter<boolean>();
    @Output() shutdown = new EventEmitter();
}

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
    let route: ActivatedRoute;
    let dotAppsService: DotAppsService;

    const messageServiceMock = new MockDotMessageService({
        'apps.search.placeholder': 'Search',
        'apps.confirmation.import.button': 'Import',
        'apps.confirmation.export.all.button': 'Export'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                DotAppsListComponent,
                MockDotAppsCardComponent,
                MockDotNotLicensedComponent,
                DotMessagePipe,
                MockDotAppsImportExportDialogComponent,
                MockDotIconComponent
            ],
            imports: [ButtonModule],
            providers: [
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
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAppsListComponent);
        component = fixture.debugElement.componentInstance;
        routerService = TestBed.inject(DotRouterService);
        route = TestBed.inject(ActivatedRoute);
        dotAppsService = TestBed.inject(DotAppsService);
    });

    describe('With access to portlet', () => {
        beforeEach(() => {
            spyOnProperty(route, 'data').and.returnValue(
                of({ dotAppsListResolverData: { apps: appsResponse, isEnterpriseLicense: true } })
            );
            fixture.detectChanges();
        });

        it('should set App from resolver', () => {
            expect(component.apps).toBe(appsResponse);
            expect(component.appsCopy).toEqual(appsResponse);
        });

        it('should contain 2 app configurations', () => {
            expect(fixture.debugElement.queryAll(By.css('dot-apps-card')).length).toBe(2);
        });

        it('should contain a dot-icon and a link with info on how to create apps', () => {
            const link = fixture.debugElement.query(By.css('.dot-apps__header-info a'));
            const icon = fixture.debugElement.query(By.css('.dot-apps__header-info dot-icon'));
            expect(link.nativeElement.href).toBe(
                'https://dotcms.com/docs/latest/apps-integrations'
            );
            expect(link.nativeElement.target).toBe('_blank');
            expect(icon.componentInstance.name).toBe('help');
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

        it('should export All button be enabled', () => {
            const exportAllBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_export_button')
            );
            expect(exportAllBtn.nativeElement.disabled).toBe(false);
        });

        it('should open confirm dialog and export All configurations', () => {
            const exportAllBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_export_button')
            );
            exportAllBtn.triggerEventHandler('click', 'Export');
            expect(component.showDialog).toBe(true);
            expect(component.importExportDialogAction).toBe('Export');
        });

        it('should open confirm dialog and import configurations', () => {
            const importBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_import_button')
            );
            importBtn.triggerEventHandler('click', 'Import');
            expect(component.showDialog).toBe(true);
            expect(component.importExportDialogAction).toBe('Import');
        });

        it('should reload apps data when resolve action from Import/Export dialog', () => {
            spyOn(dotAppsService, 'get').and.returnValue(of(appsResponse));
            const importExportDialog = fixture.debugElement.query(
                By.css('dot-apps-import-export-dialog')
            );
            importExportDialog.componentInstance.resolved.emit(true);
            expect(dotAppsService.get).toHaveBeenCalledTimes(1);
        });

        it('should set false to dialog state when closed Import/Export dialog', () => {
            const importExportDialog = fixture.debugElement.query(
                By.css('dot-apps-import-export-dialog')
            );
            importExportDialog.componentInstance.shutdown.emit();
            expect(component.showDialog).toBe(false);
        });

        it('should redirect to detail configuration list page when app Card clicked', () => {
            const card: MockDotAppsCardComponent = fixture.debugElement.queryAll(
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
