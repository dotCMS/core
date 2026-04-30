import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

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

import { DotAuthEditComponent } from '../dot-auth-edit/dot-auth-edit.component';

const ROWS: DotAuthSiteRow[] = [
    { hostId: '1', hostName: 'a.example', status: 'SITE_OVERRIDE', protocol: 'OAUTH' },
    { hostId: '2', hostName: 'b.example', status: 'SITE_OVERRIDE', protocol: 'SAML' },
    { hostId: '3', hostName: 'c.example', status: 'NOT_CONFIGURED', protocol: null },
    { hostId: '4', hostName: 'd.example', status: 'INHERITED', protocol: 'OAUTH' }
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
        // The mock functions on componentProviders are created once per factory
        // and leak call history across tests — clear them so each test starts fresh.
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
        // The `computed()` signal only re-evaluates on signal dependency
        // changes; jest.fn() mocks don't participate in reactive tracking.
        // So we only assert on the default fixture (SAML system default).
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

    describe('openSystemDialog', () => {
        it('opens the edit dialog with hostId = SYSTEM_HOST and forwards the save result to the store', () => {
            const onClose = new Subject<{
                protocol: DotAuthProtocol;
                values: Record<string, unknown>;
            }>();
            const dialog = spectator.inject(DialogService, true);
            const open = jest.spyOn(dialog, 'open').mockReturnValue({ onClose } as never);

            spectator.component.openSystemDialog();

            expect(open).toHaveBeenCalledTimes(1);
            const [target, config] = open.mock.calls[0];
            expect(target).toBe(DotAuthEditComponent);
            expect(config).toMatchObject({
                data: { hostId: 'SYSTEM_HOST' },
                closable: true,
                closeOnEscape: true
            });

            const payload = {
                protocol: 'OAUTH' as DotAuthProtocol,
                values: { clientId: 'abc' }
            };
            onClose.next(payload);
            onClose.complete();

            expect(spectator.component.store.saveSite).toHaveBeenCalledWith('SYSTEM_HOST', payload);
        });

        it('does not call saveSite when the dialog closes without a payload', () => {
            const onClose = new Subject<undefined>();
            jest.spyOn(spectator.inject(DialogService, true), 'open').mockReturnValue({
                onClose
            } as never);

            spectator.component.openSystemDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(spectator.component.store.saveSite).not.toHaveBeenCalled();
        });
    });

    describe('openSiteDialog', () => {
        it('opens the edit dialog with the row hostId and forwards saves', () => {
            const onClose = new Subject<{
                protocol: DotAuthProtocol;
                values: Record<string, unknown>;
            }>();
            const open = jest
                .spyOn(spectator.inject(DialogService, true), 'open')
                .mockReturnValue({ onClose } as never);

            spectator.component.openSiteDialog(ROWS[0]);

            const [, config] = open.mock.calls[0];
            expect(config).toMatchObject({
                data: { hostId: '1' },
                closable: true,
                closeOnEscape: true
            });

            const payload = {
                protocol: 'SAML' as DotAuthProtocol,
                values: { idpName: 'Okta' }
            };
            onClose.next(payload);
            onClose.complete();

            expect(spectator.component.store.saveSite).toHaveBeenCalledWith('1', payload);
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

        it('does not forward to setFilter until 300ms of silence', () => {
            spectator.component.onSearch('a');
            spectator.component.onSearch('ab');
            spectator.component.onSearch('abc');

            expect(spectator.component.store.setFilter).not.toHaveBeenCalled();

            jest.advanceTimersByTime(300);

            expect(spectator.component.store.setFilter).toHaveBeenCalledTimes(1);
            expect(spectator.component.store.setFilter).toHaveBeenLastCalledWith('abc');
        });

        it('dedupes back-to-back equal values (distinctUntilChanged)', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(300);
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(300);

            expect(spectator.component.store.setFilter).toHaveBeenCalledTimes(1);
        });
    });
});
