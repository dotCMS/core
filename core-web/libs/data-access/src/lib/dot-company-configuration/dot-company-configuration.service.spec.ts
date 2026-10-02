import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import {
    DotCompanyAuthType,
    DotCompanyBrandingForm,
    DotCompanyConfiguration,
    DotLoginLanguage
} from '@dotcms/dotcms-models';

import { DotCompanyConfigurationService } from './dot-company-configuration.service';

const createFakeCompanyConfiguration = (
    overrides: Partial<DotCompanyConfiguration> = {}
): DotCompanyConfiguration => ({
    companyId: 'dotcms.org',
    companyName: 'dotcms.org',
    portalURL: 'localhost',
    emailAddress: 'dotCMS Website <website@dotcms.com>',
    mx: 'dotcms.com',
    primaryColor: '#576be8',
    secondaryColor: '#2f3e6c',
    backgroundColor: '#1b3359',
    backgroundImage: null,
    loginScreenLogo: '/dA/abc/asset/logo.png',
    navBarLogo: null,
    authType: DotCompanyAuthType.EMAIL_ADDRESS,
    keyDigest: 'b0ab76ea89ce7bbcccd2ecaba4e19f4e13a5422350526cb5191c4e17f4e28317',
    languageId: 'en_US',
    timeZoneId: 'America/New_York',
    ...overrides
});

describe('DotCompanyConfigurationService', () => {
    let spectator: SpectatorHttp<DotCompanyConfigurationService>;

    const createHttp = createHttpFactory(DotCompanyConfigurationService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getConfiguration', () => {
        it('reads the configuration from the branding endpoint', () => {
            const configuration = createFakeCompanyConfiguration();
            let result: DotCompanyConfiguration | undefined;

            spectator.service.getConfiguration().subscribe((value) => (result = value));
            spectator
                .expectOne('/api/v1/configuration/branding', HttpMethod.GET)
                .flush({ entity: configuration });

            expect(result).toEqual(configuration);
        });

        it('propagates a server error', () => {
            let error: unknown;

            spectator.service.getConfiguration().subscribe({ error: (err) => (error = err) });
            spectator
                .expectOne('/api/v1/configuration/branding', HttpMethod.GET)
                .flush(null, { status: 401, statusText: 'Unauthorized' });

            expect(error).toBeDefined();
        });
    });

    describe('saveBranding', () => {
        it('sends the whole form and returns the saved configuration', () => {
            const form: DotCompanyBrandingForm = {
                portalURL: 'demo.dotcms.com',
                emailAddress: 'dotCMS Website <website@dotcms.com>',
                mx: 'dotcms.com',
                primaryColor: '#111111',
                secondaryColor: '#222222',
                backgroundColor: '#1b3359',
                loginScreenLogo: '/dA/abc/asset/logo.png'
            };
            const saved = createFakeCompanyConfiguration({ primaryColor: '#111111' });
            let result: DotCompanyConfiguration | undefined;

            spectator.service.saveBranding(form).subscribe((value) => (result = value));
            const req = spectator.expectOne('/api/v1/configuration/branding', HttpMethod.PUT);
            req.flush({ entity: saved });

            expect(req.request.body).toEqual(form);
            expect(result).toEqual(saved);
        });
    });

    describe('saveAuthType', () => {
        it('sends the auth type and returns the saved configuration', () => {
            const saved = createFakeCompanyConfiguration({ authType: DotCompanyAuthType.USER_ID });
            let result: DotCompanyConfiguration | undefined;

            spectator.service
                .saveAuthType(DotCompanyAuthType.USER_ID)
                .subscribe((value) => (result = value));
            const req = spectator.expectOne('/api/v1/configuration/authentication', HttpMethod.PUT);
            req.flush({ entity: saved });

            expect(req.request.body).toEqual({ authType: 'userId' });
            expect(result).toEqual(saved);
        });
    });

    describe('saveLocale', () => {
        it('sends language and time zone and returns the saved configuration', () => {
            const form = { languageId: 'es_ES', timeZoneId: 'Europe/Madrid' };
            const saved = createFakeCompanyConfiguration(form);
            let result: DotCompanyConfiguration | undefined;

            spectator.service.saveLocale(form).subscribe((value) => (result = value));
            const req = spectator.expectOne('/api/v1/configuration/locale', HttpMethod.PUT);
            req.flush({ entity: saved });

            expect(req.request.body).toEqual(form);
            expect(result).toEqual(saved);
        });
    });

    describe('regenerateKey', () => {
        it('returns the digest of the new key', () => {
            let result: string | undefined;

            spectator.service.regenerateKey().subscribe((value) => (result = value));
            spectator
                .expectOne('/api/v1/configuration/_regenerateKey', HttpMethod.POST)
                .flush({ entity: 'new-digest' });

            expect(result).toBe('new-digest');
        });
    });

    describe('sendTestEmail', () => {
        it('posts the sender to the validation endpoint', () => {
            let completed = false;

            spectator.service
                .sendTestEmail('dotCMS Website <website@dotcms.com>')
                .subscribe({ complete: () => (completed = true) });
            const req = spectator.expectOne(
                '/api/v1/configuration/_validateCompanyEmail',
                HttpMethod.POST
            );
            req.flush({ entity: 'Ok' });

            expect(req.request.body).toEqual({
                senderAndEmail: 'dotCMS Website <website@dotcms.com>'
            });
            expect(completed).toBe(true);
        });

        it('propagates a rejected address', () => {
            let error: unknown;

            spectator.service.sendTestEmail('not-an-email').subscribe({
                error: (err) => (error = err)
            });
            spectator
                .expectOne('/api/v1/configuration/_validateCompanyEmail', HttpMethod.POST)
                .flush(
                    { message: 'input does not match a valid e-mail pattern.' },
                    { status: 400, statusText: 'Bad Request' }
                );

            expect(error).toBeDefined();
        });
    });

    describe('getAdminLocales', () => {
        it('returns the admin UI locales from the login form endpoint', () => {
            const languages: DotLoginLanguage[] = [
                { language: 'en', country: 'US', displayName: 'English (United States)' },
                { language: 'es', country: 'ES', displayName: 'español (España)' }
            ];
            let result: DotLoginLanguage[] | undefined;

            spectator.service.getAdminLocales().subscribe((value) => (result = value));
            const req = spectator.expectOne('/api/v1/loginform', HttpMethod.POST);
            req.flush({ entity: { languages } });

            expect(req.request.body).toEqual({ messagesKey: [] });
            expect(result).toEqual(languages);
        });
    });
});
