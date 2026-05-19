import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotAnalyticsStackedBarComponent } from './dot-analytics-stacked-bar.component';

describe('DotAnalyticsStackedBarComponent', () => {
    let spectator: Spectator<DotAnalyticsStackedBarComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsStackedBarComponent
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    it('should create', () => {
        spectator.setInput({ engagedSessions: 3, notEngagedSessions: 5, totalSessions: 8 });
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should compute correct engaged width percentage', () => {
        spectator.setInput({ engagedSessions: 3, notEngagedSessions: 5, totalSessions: 8 });
        spectator.detectChanges();

        const engaged = spectator.query<HTMLElement>(byTestId('stacked-bar-engaged'));
        expect(engaged?.style.width).toBe('37.5%');
    });

    it('should compute correct not-engaged width percentage', () => {
        spectator.setInput({ engagedSessions: 3, notEngagedSessions: 5, totalSessions: 8 });
        spectator.detectChanges();

        const notEngaged = spectator.query<HTMLElement>(byTestId('stacked-bar-not-engaged'));
        expect(notEngaged?.style.width).toBe('62.5%');
    });

    it('should not render engaged segment when engagedSessions is 0', () => {
        spectator.setInput({ engagedSessions: 0, notEngagedSessions: 5, totalSessions: 5 });
        spectator.detectChanges();

        expect(spectator.query(byTestId('stacked-bar-engaged'))).not.toExist();
        expect(spectator.query(byTestId('stacked-bar-not-engaged'))).toExist();
    });

    it('should not render not-engaged segment when all sessions are engaged', () => {
        spectator.setInput({ engagedSessions: 5, notEngagedSessions: 0, totalSessions: 5 });
        spectator.detectChanges();

        expect(spectator.query(byTestId('stacked-bar-engaged'))).toExist();
        expect(spectator.query(byTestId('stacked-bar-not-engaged'))).not.toExist();
    });

    it('should show labels by default', () => {
        spectator.setInput({ engagedSessions: 3, notEngagedSessions: 5, totalSessions: 8 });
        spectator.detectChanges();

        expect(spectator.query(byTestId('stacked-bar-engaged'))?.querySelector('span')).toExist();
    });

    it('should hide labels when showLabels is false', () => {
        spectator.setInput({
            engagedSessions: 3,
            notEngagedSessions: 5,
            totalSessions: 8,
            showLabels: false
        });
        spectator.detectChanges();

        expect(
            spectator.query(byTestId('stacked-bar-engaged'))?.querySelector('span')
        ).not.toExist();
        expect(
            spectator.query(byTestId('stacked-bar-not-engaged'))?.querySelector('span')
        ).not.toExist();
    });

    it('should apply custom height to the track', () => {
        spectator.setInput({
            engagedSessions: 3,
            notEngagedSessions: 5,
            totalSessions: 8,
            height: '0.5rem'
        });
        spectator.detectChanges();

        const track = spectator.query<HTMLElement>(byTestId('stacked-bar-track'));
        expect(track?.style.height).toBe('0.5rem');
    });

    it('should use compact notation for large values', () => {
        spectator.setInput({
            engagedSessions: 320_000,
            notEngagedSessions: 320_000,
            totalSessions: 640_000
        });
        spectator.detectChanges();

        const label = spectator
            .query(byTestId('stacked-bar-engaged'))
            ?.querySelector('span')?.textContent;
        expect(label).toBeTruthy();
        expect(label).not.toBe('320000');
    });
});
