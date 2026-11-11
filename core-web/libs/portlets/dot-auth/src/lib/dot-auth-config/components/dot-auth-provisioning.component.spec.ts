import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotAuthProvisioningConfig } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthProvisioningComponent } from './dot-auth-provisioning.component';

import { DEFAULT_CONFIG } from '../store/dot-auth-config.mappers';

const TOOLTIP =
    'Comma-separated list of dotCMS role keys assigned to every provisioned user. Group mappings below add additional roles on top of these.';

const EMAIL_TOOLTIP =
    'Sign in as the existing dotCMS user with the same email address even when the provider sends no email_verified claim.';

// A fresh object literal picks up the index signature the component's input type requires.
const provisioningConfig = (): DotAuthProvisioningConfig & Record<string, unknown> => ({
    ...structuredClone(DEFAULT_CONFIG.oidc)
});

describe('DotAuthProvisioningComponent', () => {
    let spectator: Spectator<DotAuthProvisioningComponent>;

    const createComponent = createComponentFactory({
        component: DotAuthProvisioningComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dotauth.tooltip.defaultRoles': TOOLTIP,
                    'dotauth.tooltip.trustEmailWithoutVerifiedClaim': EMAIL_TOOLTIP
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: { config: provisioningConfig(), syncLabel: 'Sync on login' }
        });
    });

    describe('email linking toggle', () => {
        it('is hidden unless the host enables it (SAML has no email_verified claim)', () => {
            expect(spectator.query(byTestId('trust-email-toggle'))).not.toExist();
        });

        describe('when enabled', () => {
            beforeEach(() => {
                spectator.setInput('showEmailLinking', true);
            });

            it('renders off by default', () => {
                const input = spectator
                    .query(byTestId('trust-email-toggle'))
                    ?.querySelector('input');
                expect(input?.checked).toBe(false);
            });

            it('explains in the tooltip what is being trusted', () => {
                const tooltip = spectator.debugElement
                    .query(By.css('[data-testid="trust-email-tip"]'))
                    .injector.get(Tooltip);
                expect(tooltip.content).toBe(EMAIL_TOOLTIP);
            });

            it('emits the change under the backend key', () => {
                const emitted: unknown[] = [];
                spectator.output('fieldChange').subscribe((change) => emitted.push(change));

                spectator.click(
                    spectator
                        .query(byTestId('trust-email-toggle'))
                        ?.querySelector('input') as Element
                );

                expect(emitted.pop()).toEqual({
                    path: 'trustEmailWithoutVerifiedClaim',
                    value: true
                });
            });
        });
    });

    describe('default roles field', () => {
        it('shows a comma-separated example as the placeholder', () => {
            expect(spectator.query<HTMLInputElement>(byTestId('default-roles'))?.placeholder).toBe(
                'exampleRole1, exampleRole2'
            );
        });

        it('explains the comma-separated format in the tooltip', () => {
            const tooltip = spectator.debugElement
                .query(By.css('[data-testid="default-roles-tip"]'))
                .injector.get(Tooltip);
            expect(tooltip.content).toBe(TOOLTIP);
        });

        it('splits on commas, trims and drops empties', () => {
            const emitted: unknown[] = [];
            spectator.output('fieldChange').subscribe((change) => emitted.push(change));

            spectator.typeInElement(
                ' Editor, , Reviewer ',
                spectator.query(byTestId('default-roles')) as HTMLInputElement
            );

            expect(emitted.pop()).toEqual({ path: 'defaultRoles', value: ['Editor', 'Reviewer'] });
        });
    });
});
