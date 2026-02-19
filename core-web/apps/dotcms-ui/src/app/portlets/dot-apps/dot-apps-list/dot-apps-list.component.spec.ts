import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotAvatarDirective, DotIconComponent, DotSafeHtmlPipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotAppsCardComponent } from './dot-apps-card/dot-apps-card.component';
import { DotAppsListComponent } from './dot-apps-list.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

export class AppsServicesMock {
    get() {
        return of({});
    }
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'markdown',
    template: `
        <ng-content></ng-content>
    `
})
class MockMarkdownComponent {}

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

@Component({
    selector: 'dot-not-license',
    template: ''
})
class MockDotNotLicenseComponent {}

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
            imports: [
                DotAppsListComponent,
                MockDotAppsImportExportDialogComponent,
                MockDotIconComponent,
                MockDotNotLicenseComponent
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
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
        })
            .overrideComponent(DotAppsListComponent, {
                set: {
                    imports: [
                        CommonModule,
                        InputTextModule,
                        ButtonModule,
                        DotAppsCardComponent,
                        DotSafeHtmlPipe,
                        MockDotAppsImportExportDialogComponent,
                        MockDotNotLicenseComponent,
                        MockDotIconComponent,
                        DotPortletBaseComponent,
                        DotMessagePipe
                    ]
                }
            })
            .overrideComponent(DotAppsCardComponent, {
                set: {
                    imports: [
                        CommonModule,
                        CardModule,
                        AvatarModule,
                        BadgeModule,
                        DotIconComponent,
                        MockMarkdownComponent,
                        TooltipModule,
                        DotAvatarDirective,
                        DotMessagePipe
                    ]
                }
            })
            .compileComponents();

        fixture = TestBed.createComponent(DotAppsListComponent);
        component = fixture.debugElement.componentInstance;
        routerService = TestBed.inject(DotRouterService);
        route = TestBed.inject(ActivatedRoute);
        dotAppsService = TestBed.inject(DotAppsService);
    });

    describe('With access to portlet', () => {
        beforeEach(() => {
            Object.defineProperty(route, 'data', {
                value: of({
                    dotAppsListResolverData: { apps: appsResponse, isEnterpriseLicense: true }
                }),
                writable: true
            });
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
            jest.spyOn(dotAppsService, 'get').mockReturnValue(of(appsResponse));
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
            const card = fixture.debugElement.queryAll(By.css('dot-apps-card'))[0]
                .componentInstance;
            card.actionFired.emit(component.apps[0].key);
            expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.apps[0].key);
            expect(routerService.goToAppsConfiguration).toHaveBeenCalledTimes(1);
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
            expect(fixture.debugElement.query(By.css('dot-not-license'))).toBeTruthy();
        });
    });
});
