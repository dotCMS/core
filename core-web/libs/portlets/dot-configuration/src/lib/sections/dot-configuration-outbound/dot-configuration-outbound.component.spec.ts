import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationOutboundComponent } from './dot-configuration-outbound.component';

import { DotConfigurationStore } from '../../store/dot-configuration.store';
import { createConfigurationStoreSignals } from '../../testing/configuration-store.stub';

const createStoreStub = () => ({ ...createConfigurationStoreSignals(), patchBranding: vi.fn() });

describe('DotConfigurationOutboundComponent', () => {
    let spectator: Spectator<DotConfigurationOutboundComponent>;
    let store: ReturnType<typeof createStoreStub>;

    const createComponent = createComponentFactory({
        component: DotConfigurationOutboundComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.validation.email': 'Enter an address like Sender <a@b.com>.'
                })
            }
        ],
        detectChanges: false
    });

    const sendButton = () =>
        spectator
            .query(byTestId('configuration-send-test-mail-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        store = createStoreStub();
        spectator = createComponent({
            providers: [{ provide: DotConfigurationStore, useValue: store }]
        });
        spectator.detectChanges();
    });

    it('writes the portal URL to the store', () => {
        spectator.typeInElement('demo.dotcms.com', byTestId('configuration-portal-url'));

        expect(store.patchBranding).toHaveBeenCalledWith({ portalURL: 'demo.dotcms.com' });
    });

    it('writes the sender address to the store', () => {
        spectator.typeInElement('Acme <noreply@acme.com>', byTestId('configuration-email'));

        expect(store.patchBranding).toHaveBeenCalledWith({
            emailAddress: 'Acme <noreply@acme.com>'
        });
    });

    it('asks the page to send a test mail from the current address', () => {
        const spy = vi.spyOn(spectator.component.sendTestMail, 'emit');

        spectator.click(sendButton());

        expect(spy).toHaveBeenCalledWith('dotCMS Website <website@dotcms.com>');
    });

    describe('with an invalid address', () => {
        beforeEach(() => {
            store.errors.set({ emailAddress: 'configuration.validation.email' });
            spectator.detectChanges();
        });

        it('shows the validation message', () => {
            expect(spectator.query(byTestId('configuration-email-error'))).toHaveText(
                'Enter an address like Sender <a@b.com>.'
            );
            expect(spectator.query(byTestId('configuration-email'))).toHaveAttribute(
                'aria-invalid',
                'true'
            );
        });

        it('does not let the user send a test mail', () => {
            expect(sendButton()).toBeDisabled();
        });
    });

    it('shows the portal URL validation message', () => {
        store.errors.set({ portalURL: 'configuration.validation.email' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('configuration-portal-url-error'))).toExist();
    });
});
