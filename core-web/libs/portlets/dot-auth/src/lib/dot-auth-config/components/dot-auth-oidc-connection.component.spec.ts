import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthOidcConnectionComponent } from './dot-auth-oidc-connection.component';

import { DEFAULT_CONFIG } from '../store/dot-auth-config.mappers';

describe('DotAuthOidcConnectionComponent', () => {
    let spectator: Spectator<DotAuthOidcConnectionComponent>;

    const createComponent = createComponentFactory({
        component: DotAuthOidcConnectionComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                oidc: structuredClone(DEFAULT_CONFIG.oidc)
            }
        });
    });

    describe('advanced section groups fields', () => {
        beforeEach(() => {
            spectator.component.showAdvanced.set(true);
            spectator.detectChanges();
        });

        it('renders the groups URL and response path inputs', () => {
            expect(spectator.query(byTestId('groups-url'))).toBeTruthy();
            expect(spectator.query(byTestId('groups-response-path'))).toBeTruthy();
        });

        it('emits oidc.groupsUrl on groups URL input', () => {
            const emitted: unknown[] = [];
            spectator.output('fieldChange').subscribe((change) => emitted.push(change));

            spectator.typeInElement(
                "https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups?query=member_key_id=='{email}'",
                spectator.query(byTestId('groups-url')) as HTMLInputElement
            );

            expect(emitted.pop()).toEqual({
                path: 'oidc.groupsUrl',
                value: "https://cloudidentity.googleapis.com/v1/groups/-/memberships:searchDirectGroups?query=member_key_id=='{email}'"
            });
        });

        it('emits oidc.groupsResponsePath on response path input', () => {
            const emitted: unknown[] = [];
            spectator.output('fieldChange').subscribe((change) => emitted.push(change));

            spectator.typeInElement(
                'memberships[].groupKey.id',
                spectator.query(byTestId('groups-response-path')) as HTMLInputElement
            );

            expect(emitted.pop()).toEqual({
                path: 'oidc.groupsResponsePath',
                value: 'memberships[].groupKey.id'
            });
        });
    });

    describe('effective redirect URI', () => {
        const CALLBACK = '/api/v1/oauth/callback';

        const shown = () => spectator.query(byTestId('redirect-uri'))?.textContent?.trim();

        it('is visible while the advanced section is collapsed', () => {
            expect(spectator.component.showAdvanced()).toBe(false);
            expect(spectator.query(byTestId('redirect-uri'))).toBeTruthy();
        });

        it('falls back to the current origin when the override is empty', () => {
            expect(shown()).toBe(`${window.location.origin}${CALLBACK}`);
        });

        it('uses the configured site hostname over https when it looks like a real host', () => {
            spectator.setInput('siteHostname', 'intranet.example.com');
            expect(shown()).toBe(`https://intranet.example.com${CALLBACK}`);
        });

        it('falls back to the current origin when the site hostname is not a URL', () => {
            spectator.setInput('siteHostname', 'default');
            expect(shown()).toBe(`${window.location.origin}${CALLBACK}`);
        });

        it('prefers the override over the site hostname', () => {
            spectator.setInput('siteHostname', 'intranet.example.com');
            spectator.setInput('callbackUrl', 'https://cms.example.com');
            expect(shown()).toBe(`https://cms.example.com${CALLBACK}`);
        });

        it('explains on SYSTEM_HOST that inheriting sites use their own host when no override is set', () => {
            spectator.setInput('isSystem', true);
            expect(spectator.query(byTestId('redirect-uri-system-note'))).toBeTruthy();
            spectator.setInput('callbackUrl', 'https://cms.example.com');
            expect(spectator.query(byTestId('redirect-uri-system-note'))).toBeNull();
        });

        it('shows no inheriting-sites note on a site page', () => {
            spectator.setInput('siteHostname', 'intranet.example.com');
            expect(spectator.query(byTestId('redirect-uri-system-note'))).toBeNull();
        });

        it('uses the override when set', () => {
            spectator.setInput('callbackUrl', 'https://cms.example.com');
            expect(shown()).toBe(`https://cms.example.com${CALLBACK}`);
        });

        it('strips trailing slashes from the override', () => {
            spectator.setInput('callbackUrl', 'https://cms.example.com//');
            expect(shown()).toBe(`https://cms.example.com${CALLBACK}`);
        });

        it('does not append the callback path twice', () => {
            spectator.setInput('callbackUrl', `https://cms.example.com${CALLBACK}`);
            expect(shown()).toBe(`https://cms.example.com${CALLBACK}`);
        });

        it('copies the shown value to the clipboard', async () => {
            const writeText = vi.fn().mockResolvedValue(undefined);
            Object.defineProperty(navigator, 'clipboard', {
                value: { writeText },
                configurable: true
            });
            spectator.setInput('callbackUrl', 'https://cms.example.com');

            spectator.click(byTestId('copy-to-clipboard'));
            await Promise.resolve();

            expect(writeText).toHaveBeenCalledWith(`https://cms.example.com${CALLBACK}`);
        });
    });

    it('has no header test-connection button; discovery is the only trigger', () => {
        expect(spectator.query(byTestId('test-connection'))).toBeNull();
        expect(
            spectator
                .queryAll('p-button')
                .filter((el) => el.textContent?.includes('dotauth.action.test-connection'))
        ).toHaveLength(0);
        expect(spectator.query(byTestId('discover'))).toBeTruthy();
    });

    it('hides the groups fields while the advanced section is collapsed', () => {
        expect(spectator.query(byTestId('groups-url'))).toBeNull();
        expect(spectator.query(byTestId('groups-response-path'))).toBeNull();
    });
});
