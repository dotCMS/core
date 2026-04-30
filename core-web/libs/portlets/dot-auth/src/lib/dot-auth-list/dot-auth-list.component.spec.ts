import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import {
    DOT_AUTH_SYSTEM_HOST,
    DotAuthSiteRow,
    DotAuthSystemView,
    DotAuthProtocol,
    DotAuthStatus
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthListComponent } from './dot-auth-list.component';
import { DotAuthListStore } from './store/dot-auth-list.store';

const ROWS: DotAuthSiteRow[] = [
    { hostId: '1', hostName: 'a.example', status: 'SITE_OVERRIDE', protocol: 'OAUTH' },
    { hostId: '2', hostName: 'b.example', status: 'SITE_OVERRIDE', protocol: 'SAML' },
    { hostId: '3', hostName: 'c.example', status: 'NOT_CONFIGURED', protocol: null },
    { hostId: '4', hostName: 'd.example', status: 'INHERITED', protocol: 'OAUTH' }
];

const SYSTEM: DotAuthSystemView = { configured: true, protocol: 'SAML', headlessConfigured: false };

const MESSAGES = {
    'dotauth.row.system.label': 'All Sites (system default)',
    'dotauth.banner.system': 'System banner',
    'dotauth.search.placeholder': 'Search',
    'dotauth.table.header.site': 'Site',
    'dotauth.table.header.status': 'Status',
    'dotauth.table.header.actions': 'Actions',
    'dotauth.column.protocol': 'Protocol',
    'dotauth.protocol.oauth': 'OAuth 2.0 / OIDC',
    'dotauth.protocol.saml': 'SAML 2.0',
    'dotauth.status.site-override': 'Site override',
    'dotauth.status.inherited': 'Inherits from System',
    'dotauth.status.not-configured': 'Not configured',
    'dotauth.status.configured': 'Configured',
    'dotauth.action.edit': 'Edit',
    'dotauth.action.configure': 'Configure',
    'dotauth.action.clear': 'Clear',
    'dotauth.empty.state.title': 'No sites yet',
    'dotauth.empty.state.description': 'Once you add sites…',
    'dotauth.filter.all': 'All',
    'dotauth.filter.overrides': 'Overrides',
    'dotauth.filter.sso-on': 'SSO on',
    'dotauth.filter.headless-on': 'Headless on',
    'dotauth.filter.disabled': 'Disabled',
    Cancel: 'Cancel'
};

describe('DotAuthListComponent', () => {
    let spectator: Spectator<DotAuthListComponent>;
    let router: Router;

    const createComponent = createComponentFactory({
        component: DotAuthListComponent,
        componentProviders: [
            mockProvider(DotAuthListStore, {
                system: jest.fn().mockReturnValue(SYSTEM),
                sites: jest.fn().mockReturnValue(ROWS),
                filteredSites: jest.fn().mockReturnValue(ROWS),
                query: jest.fn().mockReturnValue(''),
                filter: jest.fn().mockReturnValue('all'),
                status: jest.fn().mockReturnValue('loaded'),
                loadSites: jest.fn(),
                setQuery: jest.fn(),
                setFilter: jest.fn(),
                clearSite: jest.fn()
            }),
            ConfirmationService
        ],
        providers: [
            mockProvider(Router),
            { provide: ActivatedRoute, useValue: {} },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        router = spectator.inject(Router);
        jest.clearAllMocks();
    });

    describe('statusTag (severity encodes protocol)', () => {
        function tag(status: DotAuthStatus, protocol: DotAuthProtocol | null) {
            return spectator.component.statusTag(status, protocol);
        }

        it('returns secondary severity for NOT_CONFIGURED regardless of protocol', () => {
            expect(tag('NOT_CONFIGURED', null).severity).toBe('secondary');
        });

        it('returns success severity for OAUTH rows', () => {
            expect(tag('SITE_OVERRIDE', 'OAUTH').severity).toBe('success');
            expect(tag('INHERITED', 'OAUTH').severity).toBe('success');
        });

        it('returns info severity for SAML rows', () => {
            expect(tag('SITE_OVERRIDE', 'SAML').severity).toBe('info');
            expect(tag('INHERITED', 'SAML').severity).toBe('info');
        });

        it('picks site-override vs inherited label from status', () => {
            expect(tag('SITE_OVERRIDE', 'OAUTH').labelKey).toBe('dotauth.status.site-override');
            expect(tag('INHERITED', 'SAML').labelKey).toBe('dotauth.status.inherited');
        });
    });

    describe('systemStatusTag', () => {
        it('renders info severity for SAML system default', () => {
            expect(spectator.component.$systemStatusTag()).toEqual({
                labelKey: 'dotauth.status.configured',
                severity: 'info'
            });
        });
    });

    describe('protocolLabelKey', () => {
        it('returns null when protocol is null', () => {
            expect(spectator.component.protocolLabelKey(null)).toBeNull();
        });

        it('returns oauth key for OAUTH', () => {
            expect(spectator.component.protocolLabelKey('OAUTH')).toBe('dotauth.protocol.oauth');
        });

        it('returns saml key for SAML', () => {
            expect(spectator.component.protocolLabelKey('SAML')).toBe('dotauth.protocol.saml');
        });
    });

    describe('Protocol column rendering', () => {
        it('renders a Protocol cell per configured row (hides when null)', () => {
            const cells = spectator.queryAll(byTestId('dotauth-protocol-cell'));
            expect(cells.length).toBe(2);
            expect(cells[0].textContent?.trim()).toContain('OAuth');
            expect(cells[1].textContent?.trim()).toContain('SAML');
        });

        it('renders the SYSTEM protocol label in the system card', () => {
            const systemProtocol = spectator.query(byTestId('dotauth-system-protocol'));
            expect(systemProtocol?.textContent?.trim()).toContain('SAML');
        });
    });

    describe('navigation', () => {
        it('openSystemConfig navigates to site/SYSTEM_HOST', () => {
            spectator.component.openSystemConfig();

            expect(router.navigate).toHaveBeenCalledWith(
                ['site', DOT_AUTH_SYSTEM_HOST],
                expect.objectContaining({ relativeTo: expect.anything() })
            );
        });

        it('openHeadlessConfig navigates to headless', () => {
            spectator.component.openHeadlessConfig();

            expect(router.navigate).toHaveBeenCalledWith(
                ['headless'],
                expect.objectContaining({ relativeTo: expect.anything() })
            );
        });

        it('openSiteConfig navigates to site/:hostId', () => {
            spectator.component.openSiteConfig('1');

            expect(router.navigate).toHaveBeenCalledWith(
                ['site', '1'],
                expect.objectContaining({ relativeTo: expect.anything() })
            );
        });
    });

    describe('confirmClearSystem / confirmClearSite', () => {
        it('clears the system row when the confirmation is accepted', () => {
            const confirm = spectator.inject(ConfirmationService, true);
            jest.spyOn(confirm, 'confirm').mockImplementation((opts) => {
                opts.accept?.();
                return confirm;
            });

            spectator.component.confirmClearSystem();

            expect(spectator.component.store.clearSite).toHaveBeenCalledWith('SYSTEM_HOST');
        });

        it('does not clear the system row when rejected', () => {
            const confirm = spectator.inject(ConfirmationService, true);
            jest.spyOn(confirm, 'confirm').mockImplementation((opts) => {
                opts.reject?.();
                return confirm;
            });

            spectator.component.confirmClearSystem();

            expect(spectator.component.store.clearSite).not.toHaveBeenCalled();
        });

        it('clears a site row when the confirmation is accepted', () => {
            const confirm = spectator.inject(ConfirmationService, true);
            jest.spyOn(confirm, 'confirm').mockImplementation((opts) => {
                opts.accept?.();
                return confirm;
            });

            spectator.component.confirmClearSite(ROWS[0]);

            expect(spectator.component.store.clearSite).toHaveBeenCalledWith('1');
        });
    });

    describe('onSearch (300ms debounce)', () => {
        beforeEach(() => jest.useFakeTimers());
        afterEach(() => jest.useRealTimers());

        it('does not forward to setQuery until 300ms of silence', () => {
            spectator.component.onSearch('a');
            spectator.component.onSearch('ab');
            spectator.component.onSearch('abc');

            expect(spectator.component.store.setQuery).not.toHaveBeenCalled();

            jest.advanceTimersByTime(300);

            expect(spectator.component.store.setQuery).toHaveBeenCalledTimes(1);
            expect(spectator.component.store.setQuery).toHaveBeenLastCalledWith('abc');
        });

        it('dedupes back-to-back equal values (distinctUntilChanged)', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(300);
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(300);

            expect(spectator.component.store.setQuery).toHaveBeenCalledTimes(1);
        });
    });
});
