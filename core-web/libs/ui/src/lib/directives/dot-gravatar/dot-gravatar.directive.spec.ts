import { createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';
import md5 from 'md5';

import { fakeAsync, tick } from '@angular/core/testing';

import { Avatar, AvatarModule } from 'primeng/avatar';

import { DotGravatarDirective } from './dot-gravatar.directive';

describe('DotGravatarDirective', () => {
    let spectator: SpectatorHost<DotGravatarDirective>;
    const createHost = createHostFactory({
        component: DotGravatarDirective,
        imports: [AvatarModule]
    });

    describe('when valid email is provided', () => {
        const testEmail = 'test@test.com';

        beforeEach(() => {
            spectator = createHost(
                `<p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>`,
                {
                    hostProps: {
                        email: testEmail
                    },
                    detectChanges: false
                }
            );
        });

        it('should generate correct gravatar URL with d=404 parameter', () => {
            spectator.detectChanges();
            const directive = spectator.component;
            const expectedHash = md5(testEmail.trim().toLowerCase());
            const expectedUrl = `https://www.gravatar.com/avatar/${expectedHash}?s=48&r=g&d=404`;

            expect(directive.$gravatarUrl()).toBe(expectedUrl);
        });

        it('should set the avatar image with gravatar URL', () => {
            spectator.detectChanges();
            const avatarComponent = spectator.queryHost(Avatar);
            const expectedHash = md5(testEmail.trim().toLowerCase());
            const expectedUrl = `https://www.gravatar.com/avatar/${expectedHash}?s=48&r=g&d=404`;

            expect(avatarComponent.image).toBe(expectedUrl);
            expect(avatarComponent.label).toBeNull();
        });

        it('should extract first letter from email', () => {
            spectator.detectChanges();
            const directive = spectator.component;
            expect(directive.$firstLetter()).toBe('T');
        });

        it('should fallback to letter when image fails to load', fakeAsync(() => {
            spectator.detectChanges();
            const avatarComponent = spectator.queryHost(Avatar);

            // Simulate image error
            avatarComponent.onImageError.emit(new Event('error'));
            tick();
            spectator.detectChanges();

            expect(avatarComponent.label).toBe('T');
            expect(avatarComponent.image).toBeNull();
        }));
    });

    describe('when email with uppercase and whitespace is provided', () => {
        const testEmail = '  TEST@TEST.COM  ';

        beforeEach(() => {
            spectator = createHost(
                `<p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>`,
                {
                    hostProps: {
                        email: testEmail
                    },
                    detectChanges: false
                }
            );
        });

        it('should trim and lowercase email before generating hash', () => {
            spectator.detectChanges();
            const directive = spectator.component;
            const expectedHash = md5('test@test.com');
            const expectedUrl = `https://www.gravatar.com/avatar/${expectedHash}?s=48&r=g&d=404`;

            expect(directive.$gravatarUrl()).toBe(expectedUrl);
        });

        it('should extract first letter correctly', () => {
            spectator.detectChanges();
            const directive = spectator.component;
            expect(directive.$firstLetter()).toBe('T');
        });
    });

    describe('when invalid email is provided (no @ symbol)', () => {
        const testEmail = 'notanemail';

        beforeEach(() => {
            spectator = createHost(
                ` <p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar> `,
                {
                    hostProps: {
                        email: testEmail
                    },
                    detectChanges: false
                }
            );
        });

        it('should return null for gravatar URL', () => {
            spectator.detectChanges();
            const directive = spectator.component;
            expect(directive.$gravatarUrl()).toBeNull();
        });

        it('should display first letter as fallback', () => {
            spectator.detectChanges();
            const avatarComponent = spectator.queryHost(Avatar);

            expect(avatarComponent.label).toBe('N');
            expect(avatarComponent.image).toBeNull();
        });
    });

    describe('when empty email is provided', () => {
        const testEmail = '';

        beforeEach(() => {
            spectator = createHost(
                `<p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>`,
                {
                    hostProps: {
                        email: testEmail
                    },
                    detectChanges: false
                }
            );
        });

        it('should return null for gravatar URL', () => {
            spectator.detectChanges();
            const directive = spectator.component;

            expect(directive.$gravatarUrl()).toBeNull();
        });

        it('should display default fallback letter "A"', () => {
            spectator.detectChanges();
            const avatarComponent = spectator.queryHost(Avatar);

            expect(avatarComponent.label).toBe('A');
            expect(avatarComponent.image).toBeNull();
        });

        it('should return "A" for first letter when email is empty', () => {
            spectator.detectChanges();
            const directive = spectator.component;

            expect(directive.$firstLetter()).toBe('A');
        });
    });

    describe('email input reactivity', () => {
        it('should update gravatar URL when email changes', () => {
            spectator = createHost(
                `<p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>`,
                {
                    hostProps: {
                        email: 'initial@test.com'
                    },
                    detectChanges: false
                }
            );
            spectator.detectChanges();

            const directive = spectator.component;
            const avatarComponent = spectator.queryHost(Avatar);
            const initialHash = md5('initial@test.com');
            const initialUrl = `https://www.gravatar.com/avatar/${initialHash}?s=48&r=g&d=404`;

            expect(avatarComponent.image).toBe(initialUrl);

            // Change email
            spectator.setHostInput({ email: 'updated@test.com' });
            spectator.detectChanges();

            const updatedHash = md5('updated@test.com');
            const updatedUrl = `https://www.gravatar.com/avatar/${updatedHash}?s=48&r=g&d=404`;

            expect(directive.$gravatarUrl()).toBe(updatedUrl);
        });

        it('should switch from image to letter when email becomes invalid', () => {
            spectator = createHost(
                ` <p-avatar
                    [email]="email"
                    dotGravatar
                    data-testid="gravatar-avatar">
                </p-avatar>`,
                {
                    hostProps: {
                        email: 'valid@email.com'
                    },
                    detectChanges: false
                }
            );
            spectator.detectChanges();

            spectator.detectChanges();
            const avatarComponent = spectator.queryHost(Avatar);

            expect(avatarComponent.image).toBeTruthy();
            expect(avatarComponent.label).toBeNull();

            // Change to invalid email
            spectator.setHostInput({ email: 'invalid' });
            spectator.detectChanges();

            expect(avatarComponent.image).toBeNull();
            expect(avatarComponent.label).toBe('I');
        });
    });
});
