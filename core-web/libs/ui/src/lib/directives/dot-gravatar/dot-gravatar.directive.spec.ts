import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import md5 from 'md5';

import { fakeAsync, tick } from '@angular/core/testing';

import { AvatarModule } from 'primeng/avatar';

import { DotGravatarDirective } from './dot-gravatar.directive';

describe('DotGravatarDirective', () => {
    let spectator: SpectatorHost<DotGravatarDirective>;
    const createHost = createHostFactory({
        component: DotGravatarDirective,
        imports: [AvatarModule]
    });

    describe('when no email is provided', () => {
        beforeEach(() => {
            spectator = createHost(`
                <p-avatar
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>
            `);
        });

        it('should set fallback values in the avatar component', () => {
            const directive = spectator.component;
            const avatarComponent = directive['avatar'];

            expect(avatarComponent.shape).toBe('circle');
            expect(avatarComponent.label).toBe('A');
        });
    });

    describe('when email is provided', () => {
        const testEmail = 'test@test.com';

        it('should set correct gravatar URL in the avatar component', fakeAsync(() => {
            spectator = createHost(
                `
                <p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>
            `,
                {
                    hostProps: {
                        email: testEmail
                    }
                }
            );
            spectator.detectChanges();
            tick();

            const directive = spectator.component;
            const expectedHash = md5(testEmail.trim().toLowerCase());
            const expectedUrl = `https://www.gravatar.com/avatar/${expectedHash}?s=48&r=g`;

            // Verify the directive properly set the values in the avatar component
            const avatarComponent = directive['avatar'];
            expect(avatarComponent.shape).toBe('circle');
            expect(avatarComponent.image).toBe(expectedUrl);
        }));
    });
});
