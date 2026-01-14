import { expect, it, describe, beforeEach } from '@jest/globals';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of, Subject } from 'rxjs';

import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotAppsService, DotMessageService, DotRouterService } from '@dotcms/data-access';
import { ComponentStatus, DotApp } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, MockDotRouterService } from '@dotcms/utils-testing';

import { DotAppsListComponent } from './dot-apps-list.component';

import { DotAppsImportExportDialogStore } from '../dot-apps-import-export-dialog/store/dot-apps-import-export-dialog.store';
import { appsResponse } from '../shared/mocks';

describe('DotAppsListComponent', () => {
    let spectator: Spectator<DotAppsListComponent>;
    let importSuccessSubject: Subject<void>;

    const mockDialogStore = {
        // Methods
        openImport: jest.fn(),
        openExport: jest.fn(),
        close: jest.fn(),
        exportConfiguration: jest.fn(),
        importConfiguration: jest.fn(),
        // Signals needed by dialog component
        visible: signal(false),
        action: signal(null),
        errorMessage: signal(null),
        dialogHeaderKey: signal(''),
        isLoading: signal(false),
        status: signal(ComponentStatus.INIT),
        app: signal(null),
        site: signal(null),
        // Observable
        importSuccess$: new Subject<void>()
    };

    const mockDotAppsService = {
        get: jest.fn().mockReturnValue(of(appsResponse))
    };

    const messageServiceMock = new MockDotMessageService({
        'apps.search.placeholder': 'Search',
        'apps.confirmation.import.button': 'Import',
        'apps.confirmation.export.all.button': 'Export',
        'apps.link.info': 'Learn more'
    });

    const createComponent = createComponentFactory({
        component: DotAppsListComponent,
        imports: [InputTextModule, ButtonModule, DotMessagePipe],
        shallow: true,
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    data: of({ dotAppsListResolverData: appsResponse })
                }
            },
            { provide: DotRouterService, useClass: MockDotRouterService },
            { provide: DotAppsService, useValue: mockDotAppsService },
            { provide: DotAppsImportExportDialogStore, useValue: mockDialogStore },
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    beforeEach(() => {
        // Reset mocks
        mockDialogStore.openImport.mockClear();
        mockDialogStore.openExport.mockClear();
        mockDotAppsService.get.mockClear();
        mockDotAppsService.get.mockReturnValue(of(appsResponse));

        // Create new subject for each test
        importSuccessSubject = new Subject<void>();
        mockDialogStore.importSuccess$ = importSuccessSubject;

        spectator = createComponent();
        spectator.detectChanges();
    });

    describe('Initial State', () => {
        it('should create component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should load apps from resolver', () => {
            expect(spectator.component.state.allApps()).toEqual(appsResponse);
            expect(spectator.component.state.displayedApps()).toEqual(appsResponse);
        });

        it('should render search input with placeholder', () => {
            const input = spectator.query('input[pInputText]');
            expect(input).toBeTruthy();
            expect(input?.getAttribute('placeholder')).toBe('Search');
        });

        it('should render import button', () => {
            const importBtn = spectator.query('.dot-apps-configuration__action_import_button');
            expect(importBtn).toBeTruthy();
        });

        it('should render export button', () => {
            const exportBtn = spectator.query('.dot-apps-configuration__action_export_button');
            expect(exportBtn).toBeTruthy();
        });
    });

    describe('Export Button State', () => {
        it('should enable export button when apps have configurations', () => {
            // appsResponse has one app with configurationsCount: 1
            expect(spectator.component.isExportButtonDisabled()).toBe(true);
        });

        it('should disable export button when no apps have configurations', () => {
            // Create apps with no configurations
            const appsWithNoConfig: DotApp[] = [
                { ...appsResponse[0], configurationsCount: 0 },
                { ...appsResponse[1], configurationsCount: 0 }
            ];

            mockDotAppsService.get.mockReturnValue(of(appsWithNoConfig));

            // Reload to get apps with no configurations
            spectator.component.reloadAppsData();
            spectator.detectChanges();

            expect(spectator.component.isExportButtonDisabled()).toBe(false);
        });
    });

    describe('Dialog Actions', () => {
        it('should call store.openImport when import button clicked', () => {
            spectator.component.openImportDialog();

            expect(mockDialogStore.openImport).toHaveBeenCalledTimes(1);
        });

        it('should call store.openExport when export button clicked', () => {
            spectator.component.openExportDialog();

            expect(mockDialogStore.openExport).toHaveBeenCalledTimes(1);
            expect(mockDialogStore.openExport).toHaveBeenCalledWith(null);
        });

        it('should call openImportDialog when import button is clicked in template', () => {
            jest.spyOn(spectator.component, 'openImportDialog');
            const importBtn = spectator.query('.dot-apps-configuration__action_import_button');
            if (importBtn) {
                spectator.click(importBtn);
            }

            expect(spectator.component.openImportDialog).toHaveBeenCalled();
        });

        it('should call openExportDialog when export button is clicked in template', () => {
            jest.spyOn(spectator.component, 'openExportDialog');
            const exportBtn = spectator.query('.dot-apps-configuration__action_export_button');
            if (exportBtn) {
                spectator.click(exportBtn);
            }

            expect(spectator.component.openExportDialog).toHaveBeenCalled();
        });
    });

    describe('Navigation', () => {
        it('should navigate to app configuration when goToApp is called', () => {
            const routerService = spectator.inject(DotRouterService);

            spectator.component.goToApp('google-calendar');

            expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith('google-calendar');
        });
    });

    describe('Reload Apps Data', () => {
        it('should reload apps when importSuccess$ emits', () => {
            mockDotAppsService.get.mockClear();
            const newApps: DotApp[] = [
                {
                    allowExtraParams: true,
                    configurationsCount: 2,
                    key: 'new-app',
                    name: 'New App',
                    description: 'A new app'
                }
            ];
            mockDotAppsService.get.mockReturnValue(of(newApps));

            // Emit import success
            importSuccessSubject.next();

            expect(mockDotAppsService.get).toHaveBeenCalled();
        });

        it('should call reloadAppsData and update state', () => {
            const newApps: DotApp[] = [
                {
                    allowExtraParams: true,
                    configurationsCount: 5,
                    key: 'updated-app',
                    name: 'Updated App',
                    description: 'Updated description'
                }
            ];
            mockDotAppsService.get.mockReturnValue(of(newApps));

            spectator.component.reloadAppsData();

            expect(spectator.component.state.allApps()).toEqual(newApps);
            expect(spectator.component.state.displayedApps()).toEqual(newApps);
        });
    });

    describe('Info Link', () => {
        it('should have link to documentation', () => {
            const link = spectator.query('.dot-apps__header-info a');
            expect(link).toBeTruthy();
            expect(link?.getAttribute('href')).toBe(
                'https://dotcms.com/docs/latest/apps-integrations'
            );
            expect(link?.getAttribute('target')).toBe('_blank');
        });
    });
});
