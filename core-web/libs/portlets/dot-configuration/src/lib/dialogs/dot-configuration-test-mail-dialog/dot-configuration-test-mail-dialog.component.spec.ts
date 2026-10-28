import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationTestMailDialogComponent } from './dot-configuration-test-mail-dialog.component';

describe('DotConfigurationTestMailDialogComponent', () => {
    let spectator: Spectator<DotConfigurationTestMailDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationTestMailDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            {
                provide: DynamicDialogConfig,
                useValue: { data: { sender: 'dotCMS Website <website@dotcms.com>' } }
            },
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotCompanyConfigurationService, {
                sendTestEmail: vi.fn().mockReturnValue(of(undefined))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ],
        detectChanges: false
    });

    const sendButton = () =>
        spectator
            .query(byTestId('configuration-test-mail-send-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    beforeEach(async () => {
        spectator = createComponent();
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    });

    afterEach(() => vi.clearAllMocks());

    it('starts with the sender from the form', () => {
        expect(spectator.query(byTestId('configuration-test-mail-sender'))).toHaveValue(
            'dotCMS Website <website@dotcms.com>'
        );
    });

    it('sends from the sender and closes with it once queued', () => {
        spectator.click(sendButton());

        expect(spectator.inject(DotCompanyConfigurationService).sendTestEmail).toHaveBeenCalledWith(
            'dotCMS Website <website@dotcms.com>'
        );
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(
            'dotCMS Website <website@dotcms.com>'
        );
    });

    it('sends from an edited sender', () => {
        spectator.typeInElement(
            'Acme <noreply@acme.com>',
            byTestId('configuration-test-mail-sender')
        );
        spectator.detectChanges();

        spectator.click(sendButton());

        expect(spectator.inject(DotCompanyConfigurationService).sendTestEmail).toHaveBeenCalledWith(
            'Acme <noreply@acme.com>'
        );
    });

    it('blocks sending to a malformed address', () => {
        spectator.typeInElement('not-an-email', byTestId('configuration-test-mail-sender'));
        spectator.detectChanges();

        expect(spectator.query(byTestId('configuration-test-mail-error'))).toExist();
        expect(sendButton()).toBeDisabled();
    });

    it('stays open and reports the error when the server rejects the address', () => {
        const error = new Error('bad address');
        vi.mocked(
            spectator.inject(DotCompanyConfigurationService).sendTestEmail
        ).mockReturnValueOnce(throwError(() => error));

        spectator.click(sendButton());

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        expect(spectator.inject(DynamicDialogRef).close).not.toHaveBeenCalled();
    });

    it('sends nothing on cancel', () => {
        spectator.click(
            spectator
                .query(byTestId('configuration-test-mail-cancel-btn'))
                ?.querySelector('button') as HTMLButtonElement
        );

        expect(
            spectator.inject(DotCompanyConfigurationService).sendTestEmail
        ).not.toHaveBeenCalled();
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith();
    });
});
