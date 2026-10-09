import { DotCompanyAuthType } from '@dotcms/dotcms-models';

import {
    assetFileName,
    dirtySections,
    toBrandingForm,
    toDraft,
    toLocaleForm,
    validate
} from './dot-configuration.mappers';

import { createFakeCompanyConfiguration } from '../testing/fake-company-configuration';

describe('dot-configuration mappers', () => {
    describe('toDraft', () => {
        it('turns null values from the API into empty strings', () => {
            const draft = toDraft(
                createFakeCompanyConfiguration({ navBarLogo: null, backgroundImage: null })
            );

            expect(draft.branding.navBarLogo).toBe('');
            expect(draft.branding.backgroundImage).toBe('');
        });

        it('keeps the hidden background color so it can be sent back', () => {
            const draft = toDraft(createFakeCompanyConfiguration({ backgroundColor: '#1b3359' }));

            expect(draft.branding.backgroundColor).toBe('#1b3359');
        });
    });

    describe('toBrandingForm', () => {
        it('sends the background color the page does not show', () => {
            const { branding } = toDraft(createFakeCompanyConfiguration());

            expect(toBrandingForm(branding)).toEqual(
                expect.objectContaining({ backgroundColor: '#1b3359' })
            );
        });

        it('leaves mx out so the server derives it from the sender address', () => {
            const { branding } = toDraft(createFakeCompanyConfiguration({ mx: 'dotcms.com' }));

            expect(
                toBrandingForm({ ...branding, emailAddress: 'Acme <web@acme.com>' })
            ).not.toHaveProperty('mx');
        });

        it('omits empty optional values so the server stores them blank', () => {
            const { branding } = toDraft(createFakeCompanyConfiguration({ navBarLogo: null }));

            expect(toBrandingForm(branding).navBarLogo).toBeUndefined();
        });
    });

    describe('toLocaleForm', () => {
        it('sends language and time zone', () => {
            expect(toLocaleForm({ languageId: 'es_ES', timeZoneId: 'Europe/Madrid' })).toEqual({
                languageId: 'es_ES',
                timeZoneId: 'Europe/Madrid'
            });
        });
    });

    describe('dirtySections', () => {
        const original = toDraft(createFakeCompanyConfiguration());

        it('reports nothing when the draft matches what the server stored', () => {
            expect(dirtySections(original, original)).toEqual({
                branding: false,
                authentication: false,
                locale: false
            });
        });

        it('reports only the sections that changed', () => {
            const draft = {
                ...original,
                authType: DotCompanyAuthType.USER_ID,
                locale: { ...original.locale, timeZoneId: 'Europe/Madrid' }
            };

            expect(dirtySections(original, draft)).toEqual({
                branding: false,
                authentication: true,
                locale: true
            });
        });
    });

    describe('validate', () => {
        const valid = toDraft(createFakeCompanyConfiguration());

        it('accepts a complete configuration', () => {
            expect(validate(valid)).toEqual({});
        });

        it.each(['#abc', '#abcd', '#aabbcc', '#aabbccdd'])('accepts the hex color %s', (color) => {
            const draft = { ...valid, branding: { ...valid.branding, primaryColor: color } };

            expect(validate(draft).primaryColor).toBeUndefined();
        });

        it.each(['', 'red', '#12zz', '#12345', 'aabbcc'])('rejects the color "%s"', (color) => {
            const draft = { ...valid, branding: { ...valid.branding, secondaryColor: color } };

            expect(validate(draft).secondaryColor).toBe('configuration.validation.color');
        });

        it.each(['website@dotcms.com', 'dotCMS Website <website@dotcms.com>'])(
            'accepts the sender %s',
            (emailAddress) => {
                const draft = { ...valid, branding: { ...valid.branding, emailAddress } };

                expect(validate(draft).emailAddress).toBeUndefined();
            }
        );

        it.each(['', 'website', 'Name <website>', '<a@b> trailing'])(
            'rejects the sender "%s"',
            (emailAddress) => {
                const draft = { ...valid, branding: { ...valid.branding, emailAddress } };

                expect(validate(draft).emailAddress).toBe('configuration.validation.email');
            }
        );

        it('requires a portal URL', () => {
            const draft = { ...valid, branding: { ...valid.branding, portalURL: '  ' } };

            expect(validate(draft).portalURL).toBe('configuration.validation.required');
        });

        it('rejects markup in the portal URL', () => {
            const draft = {
                ...valid,
                branding: { ...valid.branding, portalURL: 'demo<script>' }
            };

            expect(validate(draft).portalURL).toBe('configuration.validation.portal-url');
        });

        it('requires a login screen logo', () => {
            const draft = { ...valid, branding: { ...valid.branding, loginScreenLogo: '' } };

            expect(validate(draft).loginScreenLogo).toBe('configuration.validation.required');
        });

        it('rejects a logo that is not an asset path', () => {
            const draft = {
                ...valid,
                branding: { ...valid.branding, loginScreenLogo: '/application/logo.svg' }
            };

            expect(validate(draft).loginScreenLogo).toBe('configuration.validation.asset-path');
        });

        it('accepts a bundled background', () => {
            const draft = {
                ...valid,
                branding: {
                    ...valid.branding,
                    backgroundImage: '/html/images/backgrounds/bg-11.jpg'
                }
            };

            expect(validate(draft).backgroundImage).toBeUndefined();
        });

        it('rejects a background outside the bundled set and the asset store', () => {
            const draft = {
                ...valid,
                branding: {
                    ...valid.branding,
                    backgroundImage: '/html/images/backgrounds/bg-12.jpg'
                }
            };

            expect(validate(draft).backgroundImage).toBe('configuration.validation.asset-path');
        });

        it('requires language and time zone', () => {
            const draft = { ...valid, locale: { languageId: '', timeZoneId: '' } };

            expect(validate(draft)).toEqual({
                languageId: 'configuration.validation.required',
                timeZoneId: 'configuration.validation.required'
            });
        });
    });

    describe('assetFileName', () => {
        it.each([
            ['/dA/abc-123/asset/logo.svg', 'logo.svg'],
            ['/html/images/backgrounds/bg-11.jpg', 'bg-11.jpg'],
            ['', '']
        ])('reads "%s" as "%s"', (path, fileName) => {
            expect(assetFileName(path)).toBe(fileName);
        });
    });
});
