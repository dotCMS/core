/* eslint-disable @typescript-eslint/no-explicit-any */

import { MarkdownModule } from 'ngx-markdown';
import { Observable, of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotAlertConfirmService,
    DotAppsService,
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

import { DotAppsConfigurationListComponent } from './dot-apps-configuration-list/dot-apps-configuration-list.component';
import { DotAppsConfigurationComponent } from './dot-apps-configuration.component';

import { DotActionButtonComponent } from '../../../../view/components/_common/dot-action-button/dot-action-button.component';
import { DotAppsImportExportDialogComponent } from '../../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';
import { DotAppsImportExportDialogStore } from '../../dot-apps-import-export-dialog/store/dot-apps-import-export-dialog.store';
import { DotAppsConfigurationResolver } from '../../services/dot-apps-configuration-resolver/dot-apps-configuration-resolver.service';
import { DotAppsConfigurationHeaderComponent } from '../dot-apps-configuration-detail/components/dot-apps-configuration-header/dot-apps-configuration-header.component';

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

const activatedRouteMock = {
    data: of({ data: appData }),
    snapshot: {
        data: {
            data: appData
        }
    }
};

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
    let dialogStore: InstanceType<typeof DotAppsImportExportDialogStore>;
    let paginationService: PaginatorService;
    let appsServices: DotAppsService;
    let routerService: DotRouterService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                InputTextModule,
                ButtonModule,
                CommonModule,
                DotActionButtonComponent,
                DotAppsConfigurationHeaderComponent,
                DotAppsImportExportDialogComponent,
                DotAppsConfigurationListComponent,
                DotSafeHtmlPipe,
                DotMessagePipe,
                MarkdownModule.forRoot(),
                DotAppsConfigurationComponent
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: activatedRouteMock
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
        dialogStore = TestBed.inject(DotAppsImportExportDialogStore);
        paginationService = TestBed.inject(PaginatorService);
        appsServices = TestBed.inject(DotAppsService);
        routerService = TestBed.inject(DotRouterService);
    }));

    describe('With integrations count', () => {
        let setExtraParamsSpy: jest.SpyInstance;
        let getWithOffsetSpy: jest.SpyInstance;

        beforeEach(() => {
            // Set up spies BEFORE detectChanges triggers ngOnInit
            setExtraParamsSpy = jest.spyOn(paginationService, 'setExtraParams');
            getWithOffsetSpy = jest
                .spyOn(paginationService, 'getWithOffset')
                .mockReturnValue(of(appData));

            // First detectChanges triggers ngOnInit which loads app data
            fixture.detectChanges();
            // Second detectChanges updates the template now that app signal has value
            fixture.detectChanges();
        });

        afterEach(() => {
            setExtraParamsSpy.mockClear();
            getWithOffsetSpy.mockClear();
        });

        it('should set App from resolver', () => {
            expect(component.$app().key).toBe(appData.key);
            expect(component.$app().name).toBe(appData.name);
        });

        it('should set onInit Pagination Service with right values', () => {
            expect(paginationService.url).toBe(`v1/apps/${component.$app().key}`);
            expect(paginationService.paginationPerPage).toBe(component.$paginationPerPage());
            expect(paginationService.sortField).toBe('name');
            expect(paginationService.sortOrder).toBe(1);
            expect(setExtraParamsSpy).toHaveBeenCalledWith('filter', '');
            expect(setExtraParamsSpy).toHaveBeenCalledTimes(1);
        });

        it('should call first pagination call onInit', () => {
            expect(getWithOffsetSpy).toHaveBeenCalledWith(0);
            expect(getWithOffsetSpy).toHaveBeenCalledTimes(1);
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
            expect(listComp.siteConfigurations).toEqual(component.$app().sites);
            expect(listComp.itemsPerPage).toBe(component.$paginationPerPage());
        });

        it('should dot-apps-configuration-list emit action to load more data', () => {
            // Clear the spy to only count calls from this specific test
            getWithOffsetSpy.mockClear();

            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.loadData.emit({ first: 10 });
            expect(getWithOffsetSpy).toHaveBeenCalledWith(10);
            expect(getWithOffsetSpy).toHaveBeenCalledTimes(1);
        });

        it('should redirect to goto configuration page action', () => {
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.edit.emit(sites[0]);
            expect(routerService.goToUpdateAppsConfiguration).toHaveBeenCalledWith(
                component.$app().key,
                sites[0]
            );
        });

        it('should open export dialog for all configurations', () => {
            const openExportSpy = jest.spyOn(dialogStore, 'openExport');
            const exportAllBtn = fixture.debugElement.query(
                By.css('.dot-apps-configuration__action_export_button')
            );
            exportAllBtn.triggerEventHandler('click', null);
            expect(openExportSpy).toHaveBeenCalledWith(component.$app(), undefined);
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
            expect(appsServices.deleteAllConfigurations).toHaveBeenCalledWith(component.$app().key);
            expect(appsServices.deleteAllConfigurations).toHaveBeenCalledTimes(1);
        });

        it('should export a specific configuration', () => {
            const openExportSpy = jest.spyOn(dialogStore, 'openExport');
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.export.emit(sites[0]);
            expect(openExportSpy).toHaveBeenCalledWith(component.$app(), sites[0]);
        });

        it('should delete a specific configuration', () => {
            jest.spyOn(appsServices, 'deleteConfiguration').mockReturnValue(of(null));
            const listComp = fixture.debugElement.query(
                By.css('dot-apps-configuration-list')
            ).componentInstance;
            listComp.delete.emit(sites[0]);

            expect(appsServices.deleteConfiguration).toHaveBeenCalledWith(
                component.$app().key,
                sites[0].id
            );
        });

        it('should call App filter on search', fakeAsync(() => {
            // Clear the spy to only count calls from this specific test
            setExtraParamsSpy.mockClear();

            component.$searchInputElement().nativeElement.value = 'test';
            component.$searchInputElement().nativeElement.dispatchEvent(new Event('keyup'));
            tick(550);
            expect(setExtraParamsSpy).toHaveBeenCalledWith('filter', 'test');
            expect(setExtraParamsSpy).toHaveBeenCalledTimes(1);
            expect(getWithOffsetSpy).toHaveBeenCalled();
        }));
    });
});
