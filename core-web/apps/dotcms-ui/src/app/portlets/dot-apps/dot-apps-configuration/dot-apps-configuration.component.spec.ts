/* eslint-disable @typescript-eslint/no-explicit-any */

import { MarkdownModule } from 'ngx-markdown';
import { Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotAlertConfirmService,
    DotMessageService,
    DotRouterService,
    PaginatorService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotAppsConfigurationListModule } from './dot-apps-configuration-list/dot-apps-configuration-list.module';
import { DotAppsConfigurationResolver } from './dot-apps-configuration-resolver.service';
import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';

import { DotAppsService } from '../../../api/services/dot-apps/dot-apps.service';
import { DotActionButtonModule } from '../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotAppsConfigurationHeaderModule } from '../dot-apps-configuration-header/dot-apps-configuration-header.module';
import { DotAppsImportExportDialogModule } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.module';

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
    'apps.search.placeholder': 'Search by name',
    'apps.confirmation.export.all.button': 'Export All'
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

const appData = {
    allowExtraParams: true,
    configurationsCount: 2,
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

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
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
                DotActionButtonModule,
                DotAppsConfigurationHeaderModule,
                DotAppsImportExportDialogModule,
                DotAppsConfigurationListModule,
                HttpClientTestingModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                MarkdownModule.forRoot()
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
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotAppsConfigurationResolver,
                PaginatorService,
                DotAlertConfirmService,
                ConfirmationService
            ]
        });

        fixture = TestBed.createComponent(DotAppsConfigurationComponent);
        component = fixture.debugElement.componentInstance;
        dialogService = TestBed.inject(DotAlertConfirmService);
        paginationService = TestBed.inject(PaginatorService);
        appsServices = TestBed.inject(DotAppsService);
        routerService = TestBed.inject(DotRouterService);
    }));

    describe('With integrations count', () => {
        beforeEach(() => {
            jest.spyOn(paginationService, 'setExtraParams');
            jest.spyOn<any>(paginationService, 'getWithOffset').mockReturnValue(of(appData));
            jest.spyOn(component.searchInput.nativeElement, 'focus');
            fixture.detectChanges();
        });

        it('should set App from resolver', () => {
            expect(component.apps).toBe(appData);
        });

        it('should set params in export dialog attribute', () => {
            const importExportDialog = fixture.debugElement.query(
                By.css('dot-apps-import-export-dialog')
            );
            expect(importExportDialog.componentInstance.app).toEqual(appData);
            expect(importExportDialog.componentInstance.action).toEqual('Export');
        });

        it('should set onInit Pagination Service with right values', () => {
            expect(paginationService.url).toBe(`v1/apps/${component.apps.key}`);
            expect(paginationService.paginationPerPage).toBe(component.paginationPerPage);
            expect(paginationService.sortField).toBe('name');
            expect(paginationService.sortOrder).toBe(1);
            expect(paginationService.setExtraParams).toHaveBeenCalledWith('filter', '');
            expect(paginationService.setExtraParams).toHaveBeenCalledTimes(1);
        });

        it('should call first pagination call onInit', () => {
            expect(paginationService.getWithOffset).toHaveBeenCalledWith(0);
            expect(paginationService.getWithOffset).toHaveBeenCalledTimes(1);
        });

        it('should input search be focused on init', () => {
            expect(component.searchInput.nativeElement.focus).toHaveBeenCalledTimes(1);
        });

        it('should set messages/values in DOM correctly', () => {
            expect(
                fixture.debugElement.query(By.css('.dot-apps-configuration__action_header input'))
                    .nativeElement.placeholder
            ).toContain(messageServiceMock.get('apps.search.placeholder'));

            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration__action_header button')
                )[0].nativeElement.textContent
            ).toContain(messageServiceMock.get('apps.confirmation.export.all.button'));

            expect(
                fixture.debugElement.queryAll(
                    By.css('.dot-apps-configuration__action_header button')
                )[1].nativeElement.textContent
            ).toContain(messageServiceMock.get('apps.confirmation.delete.all.button'));
        });

        it('should have dot-apps-configuration-list with correct values', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            fixture.detectChanges();
            expect(listComp.siteConfigurations).toBe(component.apps.sites);
            expect(listComp.hideLoadDataButton).toBe(true);
            expect(listComp.itemsPerPage).toBe(component.paginationPerPage);
        });

        it('should dot-apps-configuration-list emit action to load more data', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.loadData.emit({ first: 10 });
            expect(paginationService.getWithOffset).toHaveBeenCalledWith(10);
            expect(paginationService.getWithOffset).toHaveBeenCalledTimes(1);
        });

        it('should redirect to goto configuration page action', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.edit.emit(sites[0]);
            expect(routerService.goToUpdateAppsConfiguration).toHaveBeenCalledWith(
                component.apps.key,
                sites[0]
            );
        });

        it('should open confirm dialog and export All configurations', () => {
            const exportAllBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_export_button')
            );
            exportAllBtn.triggerEventHandler('click', null);
            expect(component.importExportDialog.show).toBe(true);
            expect(component.importExportDialog.site).toBeUndefined();
        });

        it('should open confirm dialog and delete All configurations', () => {
            const deleteAllBtn = fixture.debugElement.queryAll(
                By.css('.dot-apps-configuration__action_header button')
            )[1];

            jest.spyOn(dialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });

            jest.spyOn(appsServices, 'deleteAllConfigurations').mockReturnValue(of(null));

            deleteAllBtn.triggerEventHandler('click', null);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(appsServices.deleteAllConfigurations).toHaveBeenCalledWith(component.apps.key);
            expect(appsServices.deleteAllConfigurations).toHaveBeenCalledTimes(1);
        });

        it('should export a specific configuration', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.export.emit(sites[0]);
            expect(component.importExportDialog.show).toBe(true);
            expect(component.siteSelected).toBe(sites[0]);
        });

        it('should delete a specific configuration', () => {
            jest.spyOn(appsServices, 'deleteConfiguration').mockReturnValue(of(null));
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.delete.emit(sites[0]);

            expect(appsServices.deleteConfiguration).toHaveBeenCalledWith(
                component.apps.key,
                sites[0].id
            );
        });

        it('should call App filter on search', fakeAsync(() => {
            component.searchInput.nativeElement.value = 'test';
            component.searchInput.nativeElement.dispatchEvent(new Event('keyup'));
            tick(550);
            expect(paginationService.setExtraParams).toHaveBeenCalledWith('filter', 'test');
            expect(paginationService.setExtraParams).toHaveBeenCalledTimes(1);
            expect(paginationService.getWithOffset).toHaveBeenCalled();
        }));
    });
});
