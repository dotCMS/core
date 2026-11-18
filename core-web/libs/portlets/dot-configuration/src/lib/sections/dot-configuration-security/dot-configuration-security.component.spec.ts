import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { Select } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { DotCompanyAuthType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationSecurityComponent } from './dot-configuration-security.component';

import { DotConfigurationStore } from '../../store/dot-configuration.store';
import { createConfigurationStoreSignals } from '../../testing/configuration-store.stub';

const createStoreStub = () => ({ ...createConfigurationStoreSignals(), setAuthType: vi.fn() });

describe('DotConfigurationSecurityComponent', () => {
    let spectator: Spectator<DotConfigurationSecurityComponent>;
    let store: ReturnType<typeof createStoreStub>;

    const createComponent = createComponentFactory({
        component: DotConfigurationSecurityComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.security.auth-type.email': 'Email Address',
                    'configuration.security.auth-type.user-id': 'User ID',
                    'configuration.security.key-digest': 'SHA-256 key digest'
                })
            }
        ],
        detectChanges: false
    });

    const regenerateButton = () =>
        spectator
            .query(byTestId('configuration-regenerate-key-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        store = createStoreStub();
        spectator = createComponent({
            providers: [{ provide: DotConfigurationStore, useValue: store }]
        });
        spectator.detectChanges();
    });

    describe('authentication type', () => {
        it('offers email address and user id', () => {
            expect(spectator.query(Select)?.options).toEqual([
                { label: 'Email Address', value: DotCompanyAuthType.EMAIL_ADDRESS },
                { label: 'User ID', value: DotCompanyAuthType.USER_ID }
            ]);
        });

        it('writes the chosen type to the store', () => {
            spectator.triggerEventHandler(
                '[data-testid="configuration-auth-type"]',
                'ngModelChange',
                DotCompanyAuthType.USER_ID
            );

            expect(store.setAuthType).toHaveBeenCalledWith(DotCompanyAuthType.USER_ID);
        });
    });

    describe('key digest', () => {
        it('shows the digest read-only, labelled as SHA-256', () => {
            const value = spectator.query(byTestId('configuration-key-digest-value'));

            expect(value).toHaveValue(store.keyDigest());
            expect(value).toHaveAttribute('readonly');
            expect(spectator.query(byTestId('configuration-key-digest'))).toContainText(
                'SHA-256 key digest'
            );
        });

        it('is hidden when the user is not allowed to see it', () => {
            store.keyDigest.set('');
            spectator.detectChanges();

            expect(spectator.query(byTestId('configuration-key-digest'))).not.toExist();
        });

        it('asks the page to confirm a key regeneration', () => {
            const spy = vi.spyOn(spectator.component.regenerateKey, 'emit');

            spectator.click(regenerateButton());

            expect(spy).toHaveBeenCalled();
        });

        it('locks regeneration while saving', () => {
            store.saving.set(true);
            spectator.detectChanges();

            expect(regenerateButton()).toBeDisabled();
        });
    });
});
