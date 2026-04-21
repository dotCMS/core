import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import {
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
    { hostId: '2', hostName: 'b.example', status: 'INHERITED', protocol: 'SAML' },
    { hostId: '3', hostName: 'c.example', status: 'NOT_CONFIGURED', protocol: null }
];

const SYSTEM: DotAuthSystemView = { configured: true, protocol: 'SAML' };

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
    Cancel: 'Cancel'
};

describe('DotAuthListComponent', () => {
    let spectator: Spectator<DotAuthListComponent>;
    let store: jest.Mocked<InstanceType<typeof DotAuthListStore>>;

    const createComponent = createComponentFactory({
        component: DotAuthListComponent,
        componentProviders: [
            mockProvider(DotAuthListStore, {
                system: jest.fn().mockReturnValue(SYSTEM),
                sites: jest.fn().mockReturnValue(ROWS),
                filteredSites: jest.fn().mockReturnValue(ROWS),
                filter: jest.fn().mockReturnValue(''),
                status: jest.fn().mockReturnValue('loaded'),
                loadSites: jest.fn(),
                setFilter: jest.fn(),
                saveSite: jest.fn(),
                clearSite: jest.fn()
            }),
            mockProvider(DialogService),
            ConfirmationService
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAuthListStore, true) as jest.Mocked<
            InstanceType<typeof DotAuthListStore>
        >;
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
        // The `computed()` signal only re-evaluates on signal dependency
        // changes; jest.fn() mocks don't participate in reactive tracking.
        // So we only assert on the default fixture (SAML system default).
        it('renders info severity for SAML system default', () => {
            expect(spectator.component.systemStatusTag()).toEqual({
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
            // 2 configured rows (OAUTH + SAML); NOT_CONFIGURED renders the em-dash placeholder instead
            expect(cells.length).toBe(2);
            expect(cells[0].textContent?.trim()).toContain('OAuth');
            expect(cells[1].textContent?.trim()).toContain('SAML');
        });

        it('renders the SYSTEM protocol label in the system card', () => {
            const systemProtocol = spectator.query(byTestId('dotauth-system-protocol'));
            expect(systemProtocol?.textContent?.trim()).toContain('SAML');
        });
    });
});
