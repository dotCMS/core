import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotSeverityIconComponent } from './dot-severity-icon.component';

describe('DotSeverityIconComponent', () => {
    let spectator: Spectator<DotSeverityIconComponent>;

    const createComponent = createComponentFactory({
        component: DotSeverityIconComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Icon rendering based on severity', () => {
        it('should render InfoCircleIcon by default (info severity)', () => {
            spectator.detectChanges();
            const icon = spectator.query('InfoCircleIcon');

            expect(icon).toBeTruthy();
        });

        it('should render CheckIcon for success severity', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('CheckIcon');

            expect(icon).toBeTruthy();
        });

        it('should render InfoCircleIcon for info severity', () => {
            spectator.setInput('severity', 'info');
            spectator.detectChanges();
            const icon = spectator.query('InfoCircleIcon');

            expect(icon).toBeTruthy();
        });

        it('should render TimesCircleIcon for error severity', () => {
            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            const icon = spectator.query('TimesCircleIcon');

            expect(icon).toBeTruthy();
        });

        it('should render ExclamationTriangleIcon for warn severity', () => {
            spectator.setInput('severity', 'warn');
            spectator.detectChanges();
            const icon = spectator.query('ExclamationTriangleIcon');

            expect(icon).toBeTruthy();
        });
    });

    describe('Icon attributes', () => {
        it('should apply aria-hidden="true" attribute to CheckIcon', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('CheckIcon');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply data-pc-section="icon" attribute to CheckIcon', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('CheckIcon');

            expect(icon?.getAttribute('data-pc-section')).toBe('icon');
        });

        it('should apply aria-hidden="true" attribute to InfoCircleIcon', () => {
            spectator.setInput('severity', 'info');
            spectator.detectChanges();
            const icon = spectator.query('InfoCircleIcon');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply aria-hidden="true" attribute to TimesCircleIcon', () => {
            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            const icon = spectator.query('TimesCircleIcon');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply aria-hidden="true" attribute to ExclamationTriangleIcon', () => {
            spectator.setInput('severity', 'warn');
            spectator.detectChanges();
            const icon = spectator.query('ExclamationTriangleIcon');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });
    });

    describe('Severity input reactivity', () => {
        it('should switch icons when severity input changes', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            expect(spectator.query('CheckIcon')).toBeTruthy();
            expect(spectator.query('InfoCircleIcon')).toBeFalsy();

            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            expect(spectator.query('CheckIcon')).toBeFalsy();
            expect(spectator.query('TimesCircleIcon')).toBeTruthy();
        });
    });
});
