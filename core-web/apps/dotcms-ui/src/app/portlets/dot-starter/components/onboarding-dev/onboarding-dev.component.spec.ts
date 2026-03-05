import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotOnboardingDevComponent } from './onboarding-dev.component';

describe('DotOnboardingDevComponent', () => {
    let spectator: Spectator<DotOnboardingDevComponent>;

    const createComponent = createComponentFactory({
        component: DotOnboardingDevComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should render 7 framework blocks', () => {
        expect(spectator.queryAll('[data-testid="framework-block"]').length).toBe(7);
    });

    it('should render Next.js and Angular in the DOM', () => {
        const labels = spectator.queryAll('main h3').map((el) => el.textContent?.trim() ?? '');
        expect(labels).toContain('Next.js');
        expect(labels).toContain('Angular');
    });

    it('should render framework with label, image alt and CLI command', () => {
        const firstBlock = spectator.query('[data-testid="framework-block"]');
        expect(firstBlock?.querySelector('h3')?.textContent?.trim()).toBe('Next.js');
        expect(firstBlock?.querySelector('img')?.getAttribute('alt')).toBe('Next.js');
        expect(firstBlock?.querySelector('p.font-mono')?.textContent?.trim()).toBe(
            'npx @dotcms/create-app --framework=nextjs'
        );
    });
});
