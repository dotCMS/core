import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersApiTokensTabComponent } from './dot-users-api-tokens-tab.component';

import { DotApiToken, DotUsersService } from '../../../services/dot-users.service';

const MESSAGES = {
    'users.dialog.tokens.header': 'API Access Tokens',
    'users.dialog.tokens.description': 'desc',
    'users.dialog.tokens.show-inactive': 'Show inactive',
    'users.dialog.tokens.request': 'Request New Token',
    'users.dialog.tokens.revoke': 'Revoke',
    'users.dialog.tokens.revoked': 'Revoked',
    'users.dialog.tokens.expired': 'Expired',
    'users.dialog.tokens.empty': 'No active tokens.',
    'users.dialog.tokens.empty-all': 'This user has no API tokens.',
    'users.dialog.tokens.error': "We couldn't load API tokens. Please try again.",
    'users.dialog.tokens.retry': 'Retry',
    'users.dialog.tokens.column.id': 'ID',
    'users.dialog.tokens.column.label': 'Label',
    'users.dialog.tokens.column.issued': 'Issued',
    'users.dialog.tokens.column.expires': 'Expires',
    'users.dialog.tokens.column.requested-by': 'Requested By',
    'users.dialog.tokens.column.network': 'Network',
    'users.dialog.tokens.network.any': 'any',
    'users.dialog.tokens.create-mode-hint': 'Save the user first.',
    'users.dialog.tokens.reveal.title': 'API token',
    'users.dialog.tokens.reveal.description': 'copy me',
    'users.dialog.tokens.reveal.loading': 'loading',
    'users.dialog.tokens.reveal.close': 'Close',
    'users.dialog.tokens.reveal.copy': 'Copy',
    'users.dialog.tokens.reveal.copied': 'Copied!',
    'users.dialog.tokens.row.reveal-hint': 'Click to reveal the JWT'
};

function tokenFactory(overrides: Partial<DotApiToken> = {}): DotApiToken {
    return {
        id: 'tok-1',
        userId: 'user-1',
        requestingUserId: 'admin',
        requestingIp: null,
        issuer: null,
        subject: null,
        tokenType: null,
        claims: { label: 'ci' },
        allowNetwork: '0.0.0.0/0',
        issueDate: 1_700_000_000_000,
        expiresDate: 1_800_000_000_000,
        revokedDate: null,
        modificationDate: 1_700_000_000_000,
        valid: true,
        expired: false,
        revoked: false,
        ...overrides
    };
}

describe('DotUsersApiTokensTabComponent', () => {
    let spectator: Spectator<DotUsersApiTokensTabComponent>;
    let usersService: DotUsersService;
    let dialogService: DialogService;
    let confirmationService: ConfirmationService;

    // The tab declares `providers: [DialogService, ConfirmationService]`
    // — component-level DI overrides parent providers, so we can't
    // mock those here. We spy on the real instances after creation.
    // The users service and error manager come from the module tree
    // and stay mock-provided.
    const createComponent = createComponentFactory({
        component: DotUsersApiTokensTabComponent,
        imports: [NoopAnimationsModule],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotUsersService, {
                getApiTokens: jest.fn().mockReturnValue(of([])),
                getApiTokenJwt: jest.fn().mockReturnValue(of('jwt-value')),
                revokeApiToken: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        // mockProvider's jest.fn() instances are created once by the
        // factory and persist across tests. `clearAllMocks` clears
        // call history without touching the default `mockReturnValue`
        // implementations set at factory time.
        jest.clearAllMocks();
    });

    describe('create mode (no userId)', () => {
        beforeEach(() => {
            spectator = createComponent();
            usersService = spectator.inject(DotUsersService);
        });

        it('should show the create-mode hint and skip the tokens fetch', () => {
            expect(spectator.query(byTestId('users-api-tokens-create-mode-hint'))).toBeTruthy();
            expect(spectator.query(byTestId('users-api-tokens-table'))).toBeFalsy();
            expect(usersService.getApiTokens).not.toHaveBeenCalled();
        });
    });

    describe('edit mode (with userId)', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { userId: 'user-1' } });
            usersService = spectator.inject(DotUsersService);
            // Component-scoped instances live on the debug element's
            // injector — spectator.inject reads from the root/module
            // injector and would miss them. Spy so we can intercept
            // dialog.open / confirmation.confirm without breaking the
            // p-confirmDialog subscription the tab template embeds.
            dialogService = spectator.fixture.debugElement.injector.get(DialogService);
            confirmationService = spectator.fixture.debugElement.injector.get(ConfirmationService);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(undefined)
            } as never);
            jest.spyOn(confirmationService, 'confirm').mockReturnValue(confirmationService);
        });

        it('should fetch tokens on init with showRevoked=false', () => {
            expect(usersService.getApiTokens).toHaveBeenCalledWith('user-1', false);
        });

        it('should refetch when Show inactive toggles', () => {
            spectator.component['onShowRevokedChange'](true);
            spectator.detectChanges();

            expect(usersService.getApiTokens).toHaveBeenLastCalledWith('user-1', true);
        });

        it('should label an expired-but-not-revoked row as Expired (not Revoked)', () => {
            (usersService.getApiTokens as jest.Mock).mockReturnValueOnce(
                of([tokenFactory({ id: 'tok-exp', expired: true, valid: false })])
            );
            spectator.component['onShowRevokedChange'](true);
            spectator.detectChanges();

            const pill = spectator.query(byTestId('users-api-tokens-status-pill-tok-exp'));
            expect(pill?.textContent?.trim()).toBe('Expired');
        });

        it('should label a revoked row as Revoked', () => {
            (usersService.getApiTokens as jest.Mock).mockReturnValueOnce(
                of([tokenFactory({ id: 'tok-rev', revoked: true, valid: false })])
            );
            spectator.component['onShowRevokedChange'](true);
            spectator.detectChanges();

            const pill = spectator.query(byTestId('users-api-tokens-status-pill-tok-rev'));
            expect(pill?.textContent?.trim()).toBe('Revoked');
        });

        it('should render the error state with Retry when the fetch fails', () => {
            const error = new Error('boom');
            (usersService.getApiTokens as jest.Mock).mockReturnValueOnce(throwError(() => error));
            spectator.component['onShowRevokedChange'](true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('users-api-tokens-error'))).toBeTruthy();
            expect(spectator.query(byTestId('users-api-tokens-empty'))).toBeFalsy();
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);

            const retryBtn = spectator
                .query(byTestId('users-api-tokens-retry-btn'))!
                .querySelector('button') as HTMLButtonElement;
            spectator.click(retryBtn);
            expect(usersService.getApiTokens).toHaveBeenLastCalledWith('user-1', true);
        });

        it('should switch the empty copy when Show inactive is on', () => {
            spectator.component['onShowRevokedChange'](true);
            spectator.detectChanges();

            const empty = spectator.query(byTestId('users-api-tokens-empty'));
            expect(empty?.textContent?.trim()).toBe('This user has no API tokens.');
        });

        it('should reveal the JWT on row click for a valid row', () => {
            const token = tokenFactory();
            spectator.component['onRowClick'](token);
            expect(usersService.getApiTokenJwt).toHaveBeenCalledWith('tok-1');
            expect(spectator.component['$revealVisible']()).toBe(true);
            expect(spectator.component['$revealJwt']()).toBe('jwt-value');
        });

        it('should ignore row clicks on inactive rows', () => {
            spectator.component['onRowClick'](tokenFactory({ revoked: true, valid: false }));
            expect(usersService.getApiTokenJwt).not.toHaveBeenCalled();
        });

        it('should reveal on Enter and Space for valid rows and ignore other keys', () => {
            const token = tokenFactory();
            const enter = new KeyboardEvent('keydown', { key: 'Enter' });
            const space = new KeyboardEvent('keydown', { key: ' ' });
            const tab = new KeyboardEvent('keydown', { key: 'Tab' });
            const enterPreventSpy = jest.spyOn(enter, 'preventDefault');
            const spacePreventSpy = jest.spyOn(space, 'preventDefault');

            spectator.component['onRowKeydown'](token, enter);
            expect(usersService.getApiTokenJwt).toHaveBeenCalledTimes(1);
            expect(enterPreventSpy).toHaveBeenCalled();

            spectator.component['onRowKeydown'](token, space);
            expect(usersService.getApiTokenJwt).toHaveBeenCalledTimes(2);
            expect(spacePreventSpy).toHaveBeenCalled();

            spectator.component['onRowKeydown'](token, tab);
            expect(usersService.getApiTokenJwt).toHaveBeenCalledTimes(2);
        });

        it('should ignore keydown reveal on inactive rows', () => {
            const enter = new KeyboardEvent('keydown', { key: 'Enter' });
            spectator.component['onRowKeydown'](
                tokenFactory({ expired: true, valid: false }),
                enter
            );
            expect(usersService.getApiTokenJwt).not.toHaveBeenCalled();
        });

        it('should surface a reveal error through httpErrorManager and hide the dialog', () => {
            const error = new Error('malformed');
            (usersService.getApiTokenJwt as jest.Mock).mockReturnValueOnce(throwError(() => error));

            spectator.component['reveal']('tok-1');

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(spectator.component['$revealVisible']()).toBe(false);
        });

        it('should stopPropagation on the Revoke button and open the confirmation', () => {
            const stopPropagation = jest.fn();
            const token = tokenFactory();
            spectator.component['revoke'](token, { stopPropagation } as unknown as MouseEvent);

            expect(stopPropagation).toHaveBeenCalled();
            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        it('should refetch after the request dialog closes with a new token', () => {
            const closeSubject = new Subject<{ jwt: string; token: DotApiToken }>();
            (dialogService.open as jest.Mock).mockReturnValueOnce({
                onClose: closeSubject.asObservable()
            });

            spectator.component['openRequestDialog']();
            const created = { jwt: 'new-jwt', token: tokenFactory({ id: 'tok-2' }) };
            closeSubject.next(created);
            closeSubject.complete();

            // Two calls: the initial effect + the reload after create.
            expect(usersService.getApiTokens).toHaveBeenCalledTimes(2);
            expect(spectator.component['$revealVisible']()).toBe(true);
            expect(spectator.component['$revealJwt']()).toBe('new-jwt');
        });
    });
});
