import { createComponentFactory, Spectator } from '@openng/spectator/jest';

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
        it('should render pi-info-circle by default (info severity)', () => {
            spectator.detectChanges();
            const icon = spectator.query('.pi-info-circle');

            expect(icon).toBeTruthy();
        });

        it('should render pi-check for success severity', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('.pi-check');

            expect(icon).toBeTruthy();
        });

        it('should render pi-info-circle for info severity', () => {
            spectator.setInput('severity', 'info');
            spectator.detectChanges();
            const icon = spectator.query('.pi-info-circle');

            expect(icon).toBeTruthy();
        });

        it('should render pi-times-circle for error severity', () => {
            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            const icon = spectator.query('.pi-times-circle');

            expect(icon).toBeTruthy();
        });

        it('should render pi-exclamation-triangle for warn severity', () => {
            spectator.setInput('severity', 'warn');
            spectator.detectChanges();
            const icon = spectator.query('.pi-exclamation-triangle');

            expect(icon).toBeTruthy();
        });
    });

    describe('Icon attributes', () => {
        it('should apply aria-hidden="true" attribute to pi-check', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('.pi-check');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply data-pc-section="icon" attribute to pi-check', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            const icon = spectator.query('.pi-check');

            expect(icon?.getAttribute('data-pc-section')).toBe('icon');
        });

        it('should apply aria-hidden="true" attribute to pi-info-circle', () => {
            spectator.setInput('severity', 'info');
            spectator.detectChanges();
            const icon = spectator.query('.pi-info-circle');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply aria-hidden="true" attribute to pi-times-circle', () => {
            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            const icon = spectator.query('.pi-times-circle');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });

        it('should apply aria-hidden="true" attribute to pi-exclamation-triangle', () => {
            spectator.setInput('severity', 'warn');
            spectator.detectChanges();
            const icon = spectator.query('.pi-exclamation-triangle');

            expect(icon?.getAttribute('aria-hidden')).toBe('true');
        });
    });

    describe('Severity input reactivity', () => {
        it('should switch icons when severity input changes', () => {
            spectator.setInput('severity', 'success');
            spectator.detectChanges();
            expect(spectator.query('.pi-check')).toBeTruthy();
            expect(spectator.query('.pi-info-circle')).toBeFalsy();

            spectator.setInput('severity', 'error');
            spectator.detectChanges();
            expect(spectator.query('.pi-check')).toBeFalsy();
            expect(spectator.query('.pi-times-circle')).toBeTruthy();
        });
    });
});
