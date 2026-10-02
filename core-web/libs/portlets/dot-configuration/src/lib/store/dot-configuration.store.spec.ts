import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mocked } from 'vitest';

import { signal } from '@angular/core';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageDisplayService,
    DotMessageService,
    DotNavLogoService,
    DotUiColorsService
} from '@dotcms/data-access';
import { ComponentStatus, DotCompanyAuthType, DotMessageSeverity } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DOT_CONFIGURATION_DEFAULTS } from './dot-configuration.mappers';
import { DotConfigurationSaveState, DotConfigurationStore } from './dot-configuration.store';

import {
    FAKE_ADMIN_LOCALES,
    createFakeCompanyConfiguration
} from '../testing/fake-company-configuration';

describe('DotConfigurationStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotConfigurationStore>>;
    let store: InstanceType<typeof DotConfigurationStore>;
    let service: Mocked<DotCompanyConfigurationService>;
    let httpErrorManager: Mocked<DotHttpErrorManagerService>;

    const configuration = createFakeCompanyConfiguration();

    const createService = createServiceFactory({
        service: DotConfigurationStore,
        providers: [
            mockProvider(DotCompanyConfigurationService, {
                getConfiguration: vi.fn().mockReturnValue(of(configuration)),
                getAdminLocales: vi.fn().mockReturnValue(of(FAKE_ADMIN_LOCALES)),
                saveBranding: vi.fn(),
                saveAuthType: vi.fn(),
                saveLocale: vi.fn(),
                regenerateKey: vi.fn()
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotMessageDisplayService, { push: vi.fn() }),
            mockProvider(DotNavLogoService, { setLogo: vi.fn() }),
            mockProvider(DotUiColorsService, { setColors: vi.fn() }),
            mockProvider(DotIframeService, { reloadColors: vi.fn() }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.regenerate-key.success': 'The company key was regenerated.'
                })
            },
            mockProvider(GlobalStore, {
                systemTimezones: signal([{ id: 'UTC', label: 'UTC', offset: 0 }]),
                systemTimezone: signal({ id: 'UTC', label: 'UTC', offset: 0 }),
                loadSystemConfig: vi.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(
            DotCompanyConfigurationService
        ) as Mocked<DotCompanyConfigurationService>;
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as Mocked<DotHttpErrorManagerService>;
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    describe('load', () => {
        it('loads the configuration and the admin locales on init', () => {
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.draft()?.branding.primaryColor).toBe('#4e65f1');
            expect(store.keyDigest()).toBe(configuration.keyDigest);
            expect(store.adminLocales()).toEqual(FAKE_ADMIN_LOCALES);
        });

        it('starts clean, with nothing to save', () => {
            expect(store.dirty()).toBe(false);
            expect(store.canSave()).toBe(false);
            expect(store.saveState()).toBe(DotConfigurationSaveState.SAVED);
        });

        it('exposes the time zones from the global store', () => {
            expect(store.timezones()).toEqual([{ id: 'UTC', label: 'UTC', offset: 0 }]);
        });

        it('reports the error and enters the error state when loading fails', () => {
            const error = new Error('boom');
            service.getConfiguration.mockReturnValueOnce(throwError(() => error));

            store.load();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });
    });

    describe('editing', () => {
        it('marks the page as unsaved after an edit', () => {
            store.patchBranding({ primaryColor: '#111111' });

            expect(store.dirty()).toBe(true);
            expect(store.saveState()).toBe(DotConfigurationSaveState.UNSAVED);
            expect(store.canSave()).toBe(true);
        });

        it('blocks Save while a field is invalid', () => {
            store.patchBranding({ primaryColor: '#12zz' });

            expect(store.errors().primaryColor).toBe('configuration.validation.color');
            expect(store.canSave()).toBe(false);
        });

        it('discard goes back to the values last saved', () => {
            store.patchBranding({ primaryColor: '#111111' });
            store.setAuthType(DotCompanyAuthType.USER_ID);

            store.discard();

            expect(store.dirty()).toBe(false);
            expect(store.draft()?.authType).toBe(DotCompanyAuthType.EMAIL_ADDRESS);
        });

        it('restore defaults puts the default colors and background in the form', () => {
            store.restoreDefaults();

            expect(store.draft()?.branding).toEqual(
                expect.objectContaining(DOT_CONFIGURATION_DEFAULTS)
            );
            expect(store.draft()?.branding.loginScreenLogo).toBe(configuration.loginScreenLogo);
            expect(service.saveBranding).not.toHaveBeenCalled();
        });
    });

    describe('save', () => {
        it('sends only the modified sections, branding before locale', () => {
            service.saveBranding.mockReturnValue(
                of(createFakeCompanyConfiguration({ primaryColor: '#111111' }))
            );
            service.saveLocale.mockReturnValue(
                of(
                    createFakeCompanyConfiguration({
                        primaryColor: '#111111',
                        timeZoneId: 'Europe/Madrid'
                    })
                )
            );
            store.patchLocale({ timeZoneId: 'Europe/Madrid' });
            store.patchBranding({ primaryColor: '#111111' });

            store.save();

            expect(service.saveAuthType).not.toHaveBeenCalled();
            expect(service.saveBranding.mock.invocationCallOrder[0]).toBeLessThan(
                service.saveLocale.mock.invocationCallOrder[0]
            );
            expect(store.dirty()).toBe(false);
            expect(store.saveState()).toBe(DotConfigurationSaveState.SAVED);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('sends the hidden background color back unchanged', () => {
            service.saveBranding.mockReturnValue(of(configuration));
            store.patchBranding({ portalURL: 'demo.dotcms.com' });

            store.save();

            expect(service.saveBranding).toHaveBeenCalledWith(
                expect.objectContaining({ backgroundColor: '#1b3359' })
            );
        });

        it('stops at the first failure, keeps that section unsaved and names it', () => {
            const error = new Error('invalid time zone');
            service.saveBranding.mockReturnValue(
                of(createFakeCompanyConfiguration({ primaryColor: '#111111' }))
            );
            service.saveLocale.mockReturnValue(throwError(() => error));
            store.patchBranding({ primaryColor: '#111111' });
            store.patchLocale({ timeZoneId: 'Mars/Olympus' });

            store.save();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.failedSection()).toBe('locale');
            expect(store.dirtyBySection()).toEqual({
                branding: false,
                authentication: false,
                locale: true
            });
            expect(store.draft()?.locale.timeZoneId).toBe('Mars/Olympus');
            expect(store.saveState()).toBe(DotConfigurationSaveState.FAILED);
        });

        it('applies saved branding to the admin chrome without a reload', () => {
            service.saveBranding.mockReturnValue(
                of(
                    createFakeCompanyConfiguration({
                        primaryColor: '#111111',
                        navBarLogo: '/dA/nav-id/asset/nav.png'
                    })
                )
            );
            store.patchBranding({ primaryColor: '#111111' });

            store.save();

            expect(spectator.inject(DotNavLogoService).setLogo).toHaveBeenCalledWith(
                '/dA/nav-id/asset/nav.png'
            );
            expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
                document.documentElement,
                { primary: '#111111', secondary: '#233f9b', background: '#1b3359' }
            );
            expect(spectator.inject(DotIframeService).reloadColors).toHaveBeenCalled();
            expect(spectator.inject(GlobalStore).loadSystemConfig).toHaveBeenCalled();
        });

        it('resets the navbar to the dotCMS logo when the override was cleared', () => {
            service.saveBranding.mockReturnValue(
                of(createFakeCompanyConfiguration({ navBarLogo: null, portalURL: 'demo' }))
            );
            store.patchBranding({ portalURL: 'demo' });

            store.save();

            expect(spectator.inject(DotNavLogoService).setLogo).toHaveBeenCalledWith('');
        });

        it('leaves the admin chrome alone when only the locale is saved', () => {
            service.saveLocale.mockReturnValue(
                of(createFakeCompanyConfiguration({ timeZoneId: 'Europe/Madrid' }))
            );
            store.patchLocale({ timeZoneId: 'Europe/Madrid' });

            store.save();

            expect(spectator.inject(DotUiColorsService).setColors).not.toHaveBeenCalled();
            expect(spectator.inject(DotNavLogoService).setLogo).not.toHaveBeenCalled();
        });

        it('leaves the admin chrome alone when the branding save fails', () => {
            service.saveBranding.mockReturnValue(throwError(() => new Error('rejected')));
            store.patchBranding({ primaryColor: '#111111' });

            store.save();

            expect(spectator.inject(DotUiColorsService).setColors).not.toHaveBeenCalled();
        });

        it('does nothing when there is nothing to save', () => {
            store.save();

            expect(service.saveBranding).not.toHaveBeenCalled();
            expect(service.saveAuthType).not.toHaveBeenCalled();
            expect(service.saveLocale).not.toHaveBeenCalled();
        });

        it('does nothing while a field is invalid', () => {
            store.patchBranding({ emailAddress: 'not-an-email' });

            store.save();

            expect(service.saveBranding).not.toHaveBeenCalled();
        });
    });

    describe('regenerateKey', () => {
        it('shows the digest of the new key', () => {
            service.regenerateKey.mockReturnValue(of('new-digest'));

            store.regenerateKey();

            expect(store.keyDigest()).toBe('new-digest');
            expect(store.regeneratingKey()).toBe(false);
        });

        it('confirms the regeneration with a success message', () => {
            service.regenerateKey.mockReturnValue(of('new-digest'));

            store.regenerateKey();

            expect(spectator.inject(DotMessageDisplayService).push).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: DotMessageSeverity.SUCCESS,
                    message: 'The company key was regenerated.'
                })
            );
        });

        it('keeps the current digest and reports the error when it fails', () => {
            const error = new Error('forbidden');
            service.regenerateKey.mockReturnValue(throwError(() => error));

            store.regenerateKey();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.keyDigest()).toBe(configuration.keyDigest);
            expect(store.regeneratingKey()).toBe(false);
        });
    });
});
