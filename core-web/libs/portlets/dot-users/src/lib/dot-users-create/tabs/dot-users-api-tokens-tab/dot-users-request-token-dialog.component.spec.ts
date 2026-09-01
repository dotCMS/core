import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersRequestTokenDialogComponent } from './dot-users-request-token-dialog.component';

import { DotApiTokenCreateResult, DotUsersService } from '../../../services/dot-users.service';

const MESSAGES = {
    'users.dialog.tokens.request.label': 'Label',
    'users.dialog.tokens.request.expires': 'Expires Date',
    'users.dialog.tokens.request.network': 'Allow Network (CIDR)',
    'users.dialog.tokens.request.submit': 'OK',
    'users.cancel': 'Cancel'
};

const CREATE_RESULT: DotApiTokenCreateResult = {
    jwt: 'jwt-value',
    token: {
        id: 'tok-1',
        userId: 'user-42',
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
        revoked: false
    }
};

function submitButton(
    spectator: Spectator<DotUsersRequestTokenDialogComponent>
): HTMLButtonElement {
    return spectator
        .query(byTestId('users-request-token-submit-btn'))!
        .querySelector('button') as HTMLButtonElement;
}

describe('DotUsersRequestTokenDialogComponent', () => {
    let spectator: Spectator<DotUsersRequestTokenDialogComponent>;
    let dialogRef: DynamicDialogRef;
    let usersService: DotUsersService;

    const createComponent = createComponentFactory({
        component: DotUsersRequestTokenDialogComponent,
        imports: [NoopAnimationsModule],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotUsersService, {
                createApiToken: jest.fn().mockReturnValue(of(CREATE_RESULT))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        dialogRef = { close: jest.fn() } as unknown as DynamicDialogRef;
        spectator = createComponent({
            providers: [
                { provide: DynamicDialogRef, useValue: dialogRef },
                { provide: DynamicDialogConfig, useValue: { data: { userId: 'user-42' } } }
            ]
        });
        usersService = spectator.inject(DotUsersService);
    });

    it('should require Label before submitting', () => {
        spectator.component.form.controls.label.setValue('');

        spectator.click(submitButton(spectator));

        expect(usersService.createApiToken).not.toHaveBeenCalled();
        expect(spectator.component.form.controls.label.touched).toBe(true);
    });

    it('should default expires to ~3 years out and reject a past date with `past`', () => {
        // Force a date in 2000 — always in the past regardless of when
        // the suite runs. Simpler than mocking Date globally.
        spectator.component.form.patchValue({ label: 'ci', expires: '2000-01-01' });

        spectator.click(submitButton(spectator));

        expect(usersService.createApiToken).not.toHaveBeenCalled();
        expect(spectator.component.form.controls.expires.errors?.['past']).toBe(true);
    });

    it('should close with the create result when the API resolves', () => {
        spectator.component.form.patchValue({ label: 'ci' });

        spectator.click(submitButton(spectator));

        expect(usersService.createApiToken).toHaveBeenCalled();
        expect(dialogRef.close).toHaveBeenCalledWith(CREATE_RESULT);
    });

    it('should send trimmed label + network and derive expirationSeconds from the local Y/M/D', () => {
        // Fix `now` to noon local on Jun 1 2030 so the derived TTL is
        // independent of the runner's timezone. The `expires` input
        // must be parsed as LOCAL midnight (not UTC) — otherwise the
        // difference would drift by the runner's offset. 10 full days
        // minus 12 hours = 9.5 days worth of seconds.
        const nowMs = new Date(2030, 5, 1, 12, 0, 0).getTime();
        jest.spyOn(Date, 'now').mockReturnValue(nowMs);

        spectator.component.form.patchValue({
            label: '  ci  ',
            expires: '2030-06-11',
            network: '  10.0.0.0/8  '
        });
        spectator.detectChanges();

        spectator.click(submitButton(spectator));

        expect(usersService.createApiToken).toHaveBeenCalledWith({
            userId: 'user-42',
            expirationSeconds: 9.5 * 24 * 60 * 60,
            network: '10.0.0.0/8',
            claims: { label: 'ci' }
        });

        (Date.now as jest.Mock).mockRestore();
    });

    it('should surface the HTTP error through httpErrorManager and stop submitting', () => {
        const error = new Error('boom');
        (usersService.createApiToken as jest.Mock).mockReturnValueOnce(throwError(() => error));

        spectator.component.form.patchValue({ label: 'ci' });
        spectator.detectChanges();
        spectator.click(submitButton(spectator));

        const handler = spectator.inject(DotHttpErrorManagerService);
        expect(handler.handle).toHaveBeenCalledWith(error);
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should close without a result when Cancel is pressed', () => {
        const cancelBtn = spectator
            .query(byTestId('users-request-token-cancel-btn'))!
            .querySelector('button') as HTMLButtonElement;
        spectator.click(cancelBtn);

        expect(dialogRef.close).toHaveBeenCalledWith();
    });
});
