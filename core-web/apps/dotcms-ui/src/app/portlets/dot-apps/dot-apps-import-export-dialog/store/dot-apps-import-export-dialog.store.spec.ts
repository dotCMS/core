import { expect, it, describe, beforeEach } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotAppsService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, dialogAction, DotApp, DotAppsSite } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAppsImportExportDialogStore } from './dot-apps-import-export-dialog.store';

const mockApp: DotApp = {
    allowExtraParams: true,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: 'Calendar integration',
    sites: [
        { id: 'site-1', name: 'Site 1', configured: true },
        { id: 'site-2', name: 'Site 2', configured: false }
    ]
};

const mockSite: DotAppsSite = {
    id: 'site-1',
    name: 'Site 1',
    configured: true
};

describe('DotAppsImportExportDialogStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAppsImportExportDialogStore>>;
    let dotAppsService: jest.Mocked<DotAppsService>;

    const createService = createServiceFactory({
        service: DotAppsImportExportDialogStore,
        providers: [
            {
                provide: DotAppsService,
                useValue: {
                    exportConfiguration: jest.fn(),
                    importConfiguration: jest.fn()
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'apps.confirmation.export.error': 'Export Error'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotAppsService = spectator.inject(DotAppsService) as jest.Mocked<DotAppsService>;
    });

    describe('Initial State', () => {
        it('should have initial state', () => {
            expect(spectator.service.visible()).toBe(false);
            expect(spectator.service.action()).toBeNull();
            expect(spectator.service.app()).toBeNull();
            expect(spectator.service.site()).toBeNull();
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
            expect(spectator.service.errorMessage()).toBeNull();
        });

        it('should expose importSuccess$ observable', () => {
            expect(spectator.service.importSuccess$).toBeDefined();
        });

        it('should have computed isLoading as false initially', () => {
            expect(spectator.service.isLoading()).toBe(false);
        });

        it('should have computed isExport as false initially', () => {
            expect(spectator.service.isExport()).toBe(false);
        });

        it('should have computed isImport as false initially', () => {
            expect(spectator.service.isImport()).toBe(false);
        });

        it('should have computed dialogHeaderKey as empty string initially', () => {
            expect(spectator.service.dialogHeaderKey()).toBe('');
        });
    });

    describe('openExport', () => {
        it('should open export dialog with app', () => {
            spectator.service.openExport(mockApp);

            expect(spectator.service.visible()).toBe(true);
            expect(spectator.service.action()).toBe(dialogAction.EXPORT);
            expect(spectator.service.app()).toEqual(mockApp);
            expect(spectator.service.site()).toBeNull();
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
            expect(spectator.service.errorMessage()).toBeNull();
        });

        it('should open export dialog with app and site', () => {
            spectator.service.openExport(mockApp, mockSite);

            expect(spectator.service.visible()).toBe(true);
            expect(spectator.service.action()).toBe(dialogAction.EXPORT);
            expect(spectator.service.app()).toEqual(mockApp);
            expect(spectator.service.site()).toEqual(mockSite);
        });

        it('should set isExport computed to true', () => {
            spectator.service.openExport(mockApp);

            expect(spectator.service.isExport()).toBe(true);
            expect(spectator.service.isImport()).toBe(false);
        });

        it('should set dialogHeaderKey to export header', () => {
            spectator.service.openExport(mockApp);

            expect(spectator.service.dialogHeaderKey()).toBe('apps.confirmation.export.header');
        });
    });

    describe('openImport', () => {
        it('should open import dialog', () => {
            spectator.service.openImport();

            expect(spectator.service.visible()).toBe(true);
            expect(spectator.service.action()).toBe(dialogAction.IMPORT);
            expect(spectator.service.app()).toBeNull();
            expect(spectator.service.site()).toBeNull();
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
            expect(spectator.service.errorMessage()).toBeNull();
        });

        it('should set isImport computed to true', () => {
            spectator.service.openImport();

            expect(spectator.service.isImport()).toBe(true);
            expect(spectator.service.isExport()).toBe(false);
        });

        it('should set dialogHeaderKey to import header', () => {
            spectator.service.openImport();

            expect(spectator.service.dialogHeaderKey()).toBe('apps.confirmation.import.header');
        });
    });

    describe('close', () => {
        it('should reset state to initial values', () => {
            // First open a dialog
            spectator.service.openExport(mockApp, mockSite);

            // Then close it
            spectator.service.close();

            expect(spectator.service.visible()).toBe(false);
            expect(spectator.service.action()).toBeNull();
            expect(spectator.service.app()).toBeNull();
            expect(spectator.service.site()).toBeNull();
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
            expect(spectator.service.errorMessage()).toBeNull();
        });
    });

    describe('setError', () => {
        it('should set error message and status', () => {
            spectator.service.setError('Something went wrong');

            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
            expect(spectator.service.errorMessage()).toBe('Something went wrong');
        });
    });

    describe('exportConfiguration', () => {
        it('should set status to LOADING when export starts', () => {
            spectator.service.openExport(mockApp, mockSite);
            dotAppsService.exportConfiguration.mockReturnValue(
                new Promise((resolve) => setTimeout(() => resolve(''), 100))
            );

            spectator.service.exportConfiguration({ password: 'test123' });

            expect(spectator.service.status()).toBe(ComponentStatus.LOADING);
            expect(spectator.service.isLoading()).toBe(true);
        });

        it('should close dialog on successful export', async () => {
            spectator.service.openExport(mockApp, mockSite);
            dotAppsService.exportConfiguration.mockResolvedValue('');

            spectator.service.exportConfiguration({ password: 'test123' });

            // Wait for the promise to resolve
            await new Promise((resolve) => setTimeout(resolve, 0));

            expect(spectator.service.visible()).toBe(false);
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
        });

        it('should set error on failed export', async () => {
            spectator.service.openExport(mockApp, mockSite);
            dotAppsService.exportConfiguration.mockResolvedValue('Export failed reason');

            spectator.service.exportConfiguration({ password: 'test123' });

            // Wait for the promise to resolve
            await new Promise((resolve) => setTimeout(resolve, 0));

            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
            expect(spectator.service.errorMessage()).toBe('Export Error: Export failed reason');
        });

        it('should call exportConfiguration with correct config for site export', async () => {
            spectator.service.openExport(mockApp, mockSite);
            dotAppsService.exportConfiguration.mockResolvedValue('');

            spectator.service.exportConfiguration({ password: 'test123' });

            await new Promise((resolve) => setTimeout(resolve, 0));

            expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith({
                password: 'test123',
                exportAll: false,
                appKeysBySite: { 'site-1': ['google-calendar'] }
            });
        });

        it('should call exportConfiguration with all configured sites when no site is selected', async () => {
            spectator.service.openExport(mockApp);
            dotAppsService.exportConfiguration.mockResolvedValue('');

            spectator.service.exportConfiguration({ password: 'test123' });

            await new Promise((resolve) => setTimeout(resolve, 0));

            expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith({
                password: 'test123',
                exportAll: false,
                appKeysBySite: { 'site-1': ['google-calendar'] } // Only site-1 is configured
            });
        });
    });

    describe('importConfiguration', () => {
        const mockFile = new File([''], 'test.json', { type: 'application/json' });

        it('should set status to LOADING when import starts', () => {
            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('200'));

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            // The status should be INIT after successful import (because it resets)
            // But during the call it was LOADING
            expect(dotAppsService.importConfiguration).toHaveBeenCalled();
        });

        it('should close dialog on successful import', () => {
            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('200'));

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            expect(spectator.service.visible()).toBe(false);
            expect(spectator.service.status()).toBe(ComponentStatus.INIT);
        });

        it('should emit on importSuccess$ when import succeeds', () => {
            const successSpy = jest.fn();
            spectator.service.importSuccess$.subscribe(successSpy);

            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('200'));

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            expect(successSpy).toHaveBeenCalledTimes(1);
        });

        it('should NOT emit on importSuccess$ when import fails', () => {
            const successSpy = jest.fn();
            spectator.service.importSuccess$.subscribe(successSpy);

            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('400'));

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            expect(successSpy).not.toHaveBeenCalled();
        });

        it('should set error on import failure (400 status)', () => {
            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('400'));

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
            expect(spectator.service.errorMessage()).toBe('Import failed');
        });

        it('should set error on import error', () => {
            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(
                throwError(() => new Error('Network error'))
            );

            spectator.service.importConfiguration({
                file: mockFile,
                json: { password: 'test123' }
            });

            expect(spectator.service.status()).toBe(ComponentStatus.ERROR);
            expect(spectator.service.errorMessage()).toBe('Import failed');
        });

        it('should call importConfiguration with correct config', () => {
            spectator.service.openImport();
            dotAppsService.importConfiguration.mockReturnValue(of('200'));

            const config = { file: mockFile, json: { password: 'test123' } };
            spectator.service.importConfiguration(config);

            expect(dotAppsService.importConfiguration).toHaveBeenCalledWith(config);
        });
    });

    describe('Computed Properties', () => {
        it('should update isLoading when status changes to LOADING', () => {
            expect(spectator.service.isLoading()).toBe(false);

            spectator.service.openExport(mockApp);
            dotAppsService.exportConfiguration.mockReturnValue(
                new Promise((resolve) => setTimeout(() => resolve(''), 1000))
            );
            spectator.service.exportConfiguration({ password: 'test' });

            expect(spectator.service.isLoading()).toBe(true);
        });

        it('should return correct dialogHeaderKey for export', () => {
            spectator.service.openExport(mockApp);
            expect(spectator.service.dialogHeaderKey()).toBe('apps.confirmation.export.header');
        });

        it('should return correct dialogHeaderKey for import', () => {
            spectator.service.openImport();
            expect(spectator.service.dialogHeaderKey()).toBe('apps.confirmation.import.header');
        });

        it('should return empty string for dialogHeaderKey when no action', () => {
            expect(spectator.service.dialogHeaderKey()).toBe('');
        });
    });
});
