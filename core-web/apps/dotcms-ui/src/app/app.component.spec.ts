import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { EMPTY } from 'rxjs';
import { vi } from 'vitest';

import { signal, WritableSignal } from '@angular/core';
import { provideRouter } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import {
    DEFAULT_COLORS,
    DotAlertConfirmService,
    DotLicenseService,
    DotNavLogoService,
    DotUiColorsService
} from '@dotcms/data-access';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { AppComponent } from './app.component';

const config = {
    colors: { primary: '#123', secondary: '#456', background: '#789' },
    logos: { navBar: 'logo-url' },
    license: { displayServerId: 'test', isCommunity: false, level: 200, levelName: 'test level' }
} as DotSystemConfig;

describe('AppComponent', () => {
    let spectator: Spectator<AppComponent>;
    let $systemConfig: WritableSignal<DotSystemConfig | null>;
    const createComponent = createComponentFactory({
        component: AppComponent,
        shallow: true,
        detectChanges: false,
        providers: [
            provideRouter([]),
            mockProvider(ConfirmationService),
            mockProvider(DotAlertConfirmService, {
                confirmDialogOpened$: EMPTY,
                confirmModel: signal(null),
                alertModel: signal(null)
            }),
            mockProvider(DotUiColorsService),
            mockProvider(DotNavLogoService),
            mockProvider(DotLicenseService)
        ]
    });

    beforeEach(() => {
        $systemConfig = signal(null);
        spectator = createComponent({
            providers: [{ provide: GlobalStore, useValue: { systemConfig: $systemConfig } }]
        });
    });

    it('renders the router outlet and alert component', () => {
        spectator.detectChanges();
        expect(spectator.query('router-outlet')).not.toBeNull();
        expect(spectator.query('dot-alert-confirm')).not.toBeNull();
    });

    it('uses default colors before authenticated configuration is available', () => {
        spectator.detectChanges();
        expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
            expect.any(HTMLElement),
            DEFAULT_COLORS
        );
        expect(spectator.inject(DotNavLogoService).setLogo).not.toHaveBeenCalled();
        expect(spectator.inject(DotLicenseService).setLicense).not.toHaveBeenCalled();
    });

    it('applies colors, logo and license from GlobalStore without loading its own configuration', () => {
        $systemConfig.set(config);
        spectator.detectChanges();
        expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
            expect.any(HTMLElement),
            config.colors
        );
        expect(spectator.inject(DotNavLogoService).setLogo).toHaveBeenCalledWith('logo-url');
        expect(spectator.inject(DotLicenseService).setLicense).toHaveBeenCalledWith(config.license);
    });

    it('tolerates partial configuration', () => {
        // Exercise a partial server response even though the legacy model requires both fields.
        $systemConfig.set({ ...config, logos: null, license: null } as unknown as DotSystemConfig);
        spectator.detectChanges();
        expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
            expect.any(HTMLElement),
            config.colors
        );
        expect(spectator.inject(DotNavLogoService).setLogo).not.toHaveBeenCalled();
        expect(spectator.inject(DotLicenseService).setLicense).not.toHaveBeenCalled();
    });

    it('restores configured branding when post-login loading succeeds', () => {
        spectator.detectChanges();
        $systemConfig.set(config);
        spectator.detectChanges();
        expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenLastCalledWith(
            expect.any(HTMLElement),
            config.colors
        );
        expect(spectator.inject(DotNavLogoService).setLogo).toHaveBeenCalledWith('logo-url');
    });

    it('stops applying configuration after destruction', () => {
        spectator.detectChanges();
        const logo = spectator.inject(DotNavLogoService);
        spectator.fixture.destroy();
        $systemConfig.set(config);
        expect(logo.setLogo).not.toHaveBeenCalled();
    });

    it('tolerates a missing document root', () => {
        const query = vi.spyOn(document, 'querySelector').mockReturnValue(null);
        spectator.detectChanges();
        expect(spectator.inject(DotUiColorsService).setColors).not.toHaveBeenCalled();
        query.mockRestore();
    });
});
