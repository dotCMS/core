import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { By } from '@angular/platform-browser';

import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthProvisioningComponent } from './dot-auth-provisioning.component';

import { DEFAULT_CONFIG } from '../store/dot-auth-config.mappers';

const TOOLTIP =
    'Comma-separated list of dotCMS role keys assigned to every provisioned user. Group mappings below add additional roles on top of these.';

describe('DotAuthProvisioningComponent', () => {
    let spectator: Spectator<DotAuthProvisioningComponent>;

    const createComponent = createComponentFactory({
        component: DotAuthProvisioningComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ 'dotauth.tooltip.defaultRoles': TOOLTIP })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: { config: structuredClone(DEFAULT_CONFIG.oidc), syncLabel: 'Sync on login' }
        });
    });

    describe('default roles field', () => {
        it('shows a comma-separated example as the placeholder', () => {
            expect(
                spectator.query<HTMLInputElement>(byTestId('default-roles'))?.placeholder
            ).toBe('exampleRole1, exampleRole2');
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
