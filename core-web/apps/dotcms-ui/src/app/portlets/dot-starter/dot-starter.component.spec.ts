import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, EventEmitter, Output } from '@angular/core';
import { provideRouter } from '@angular/router';

import { DotStarterComponent } from './dot-starter.component';

@Component({
    selector: 'dot-onboarding-dev',
    template:
        '<button data-testid="reset-profile" (click)="eventEmitter.emit(\'reset-user-profile\')">Reset</button>',
    standalone: true
})
class StubOnboardingDev {
    @Output() eventEmitter = new EventEmitter<'reset-user-profile'>();
}

@Component({
    selector: 'dot-onboarding-author',
    template:
        '<button data-testid="reset-profile" (click)="eventEmitter.emit(\'reset-user-profile\')">Reset</button>',
    standalone: true
})
class StubOnboardingAuthor {
    @Output() eventEmitter = new EventEmitter<'reset-user-profile'>();
}

describe('DotStarterComponent', () => {
    let spectator: Spectator<DotStarterComponent>;

    const createComponent = createComponentFactory({
        component: DotStarterComponent,
        overrideComponents: [
            [DotStarterComponent, { set: { imports: [StubOnboardingDev, StubOnboardingAuthor] } }]
        ],
        providers: [provideRouter([])]
    });

    describe('when no profile in localStorage', () => {
        beforeEach(() => {
            localStorage.removeItem('user_profile');
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.element).toBeTruthy();
        });

        it('should render profile selection with heading, paragraph and role cards', () => {
            expect(spectator.query('[data-testid="profile-selection"]')).toBeTruthy();
            expect(spectator.query('h1.heading')?.textContent?.trim()).toBe('Welcome to dotCMS');
            expect(spectator.query('[data-testid="developer-card"]')).toBeTruthy();
            expect(spectator.query('[data-testid="marketer-card"]')).toBeTruthy();
            expect(
                spectator.query('[data-testid="developer-card"] .card-heading')?.textContent?.trim()
            ).toBe('Developer');
            expect(
                spectator.query('[data-testid="marketer-card"] .card-heading')?.textContent?.trim()
            ).toBe('Marketer');
            expect(spectator.query('dot-onboarding-dev')).toBeFalsy();
            expect(spectator.query('dot-onboarding-author')).toBeFalsy();
        });

        it('should show onboarding-dev and hide profile selection when developer card is clicked', () => {
            spectator.click(spectator.query('[data-testid="developer-card"]'));
            spectator.detectChanges();

            expect(spectator.query('[data-testid="profile-selection"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeTruthy();
            expect(spectator.query('[data-testid="onboarding-author"]')).toBeFalsy();
            expect(localStorage.getItem('user_profile')).toBe('developer');
        });

        it('should show onboarding-author and hide profile selection when marketer card is clicked', () => {
            spectator.click(spectator.query('[data-testid="marketer-card"]'));
            spectator.detectChanges();

            expect(spectator.query('[data-testid="profile-selection"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-author"]')).toBeTruthy();
            expect(localStorage.getItem('user_profile')).toBe('marketer');
        });
    });

    describe('when profile is developer in localStorage', () => {
        beforeEach(() => {
            localStorage.setItem('user_profile', 'developer');
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should render onboarding-dev and not profile selection on init', () => {
            expect(spectator.query('[data-testid="profile-selection"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeTruthy();
            expect(spectator.query('[data-testid="onboarding-author"]')).toBeFalsy();
        });
    });

    describe('when profile is marketer in localStorage', () => {
        beforeEach(() => {
            localStorage.setItem('user_profile', 'marketer');
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should render onboarding-author and not profile selection on init', () => {
            expect(spectator.query('[data-testid="profile-selection"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-author"]')).toBeTruthy();
        });
    });

    describe('onUserProfileReset (eventEmitter binding)', () => {
        beforeEach(() => {
            localStorage.setItem('user_profile', 'developer');
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should show profile selection and hide onboarding when reset button is clicked', () => {
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeTruthy();

            spectator.click(spectator.query('[data-testid="reset-profile"]'));
            spectator.detectChanges();

            expect(spectator.query('[data-testid="profile-selection"]')).toBeTruthy();
            expect(spectator.query('[data-testid="onboarding-dev"]')).toBeFalsy();
            expect(spectator.query('[data-testid="onboarding-author"]')).toBeFalsy();
        });
    });
});
