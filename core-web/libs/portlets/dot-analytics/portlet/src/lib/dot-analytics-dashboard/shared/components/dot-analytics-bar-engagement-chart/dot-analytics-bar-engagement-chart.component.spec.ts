import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { EngagementPlatformMetrics } from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsBarEngagementChartComponent } from './dot-analytics-bar-engagement-chart.component';

const SAMPLE_DATA: EngagementPlatformMetrics[] = [
    {
        name: 'Chrome',
        views: 3,
        percentage: 38,
        totalSessions: 8,
        time: '2m 10s'
    },
    {
        name: 'Safari',
        views: 1,
        percentage: 50,
        totalSessions: 2,
        time: '0m 50s'
    },
    {
        name: 'Firefox',
        views: 1,
        percentage: 50,
        totalSessions: 2,
        time: '1m 30s'
    },
    {
        name: 'Opera',
        views: 1,
        percentage: 100,
        totalSessions: 1,
        time: '1m 05s'
    }
];

describe('DotAnalyticsBarEngagementChartComponent', () => {
    let spectator: Spectator<DotAnalyticsBarEngagementChartComponent>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsBarEngagementChartComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn((key: string, ...args: string[]) => {
                        if (
                            key === 'analytics.engagement.charts.sessions-label' ||
                            key === 'analytics.engagement.charts.session-label'
                        ) {
                            return 'sess.';
                        }

                        return args.length ? `${key}[${args.join(',')}]` : key;
                    })
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput({
            data: SAMPLE_DATA,
            status: ComponentStatus.LOADED,
            title: ''
        });
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render list and legend when loaded with data', () => {
        expect(spectator.query(byTestId('analytics-bar-engagement-chart-body'))).toExist();
        expect(spectator.query(byTestId('analytics-bar-engagement-legend'))).toExist();
        const rows = spectator.queryAll(byTestId('analytics-bar-engagement-row'));
        expect(rows.length).toBe(SAMPLE_DATA.length);
    });

    it('should sort rows by totalSessions descending (Chrome before Safari/Firefox before Opera)', () => {
        const labels = spectator
            .queryAll(byTestId('analytics-bar-engagement-row'))
            .map((row) => row.querySelector('.bar-engagement-row__label')?.textContent?.trim());

        expect(labels.indexOf('Chrome')).toBe(0);
        expect(labels.includes('Safari')).toBe(true);
        expect(labels.includes('Firefox')).toBe(true);
        expect(labels.indexOf('Opera')).toBe(labels.length - 1);
    });

    it('should show skeleton when LOADING', () => {
        spectator.setInput({ status: ComponentStatus.LOADING });
        spectator.detectChanges();

        expect(spectator.query(byTestId('analytics-bar-engagement-chart-skeleton'))).toExist();
        expect(spectator.query(byTestId('analytics-bar-engagement-list'))).not.toExist();
    });

    it('should show error state when ERROR', () => {
        spectator.setInput({ status: ComponentStatus.ERROR });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-state-message')).toExist();
        expect(spectator.query(byTestId('analytics-bar-engagement-list'))).not.toExist();
    });

    it('should show empty state when data is empty and LOADED', () => {
        spectator.setInput({ data: [], status: ComponentStatus.LOADED });
        spectator.detectChanges();

        expect(spectator.query('dot-analytics-empty-state')).toExist();
        expect(spectator.query(byTestId('analytics-bar-engagement-list'))).not.toExist();
    });

    it('should show only top 5 rows when more than 5 items exist', () => {
        const many: EngagementPlatformMetrics[] = Array.from({ length: 6 }, (_, i) => ({
            name: `Browser-${i}`,
            views: 1,
            percentage: 10,
            totalSessions: 20 - i,
            time: '1m'
        }));
        spectator.setInput({ data: many });
        spectator.detectChanges();

        expect(spectator.queryAll(byTestId('analytics-bar-engagement-row')).length).toBe(5);
        expect(spectator.query('.bar-engagement-row__label')?.textContent?.trim()).toBe(
            'Browser-0'
        );
    });

    it('should use full-width stacked bars per row (engaged share of row totalSessions)', () => {
        spectator.detectChanges();
        const firstTrack = spectator
            .query(byTestId('analytics-bar-engagement-row'))
            ?.querySelector('.bar-engagement-row__track');
        expect(firstTrack).toExist();
        const engaged = firstTrack?.querySelector<HTMLElement>(
            '.bar-engagement-row__segment--engaged'
        );
        const notEngaged = firstTrack?.querySelector<HTMLElement>(
            '.bar-engagement-row__segment--not-engaged'
        );
        expect(engaged?.style.width).toBe('37.5%');
        expect(notEngaged?.style.width).toBe('62.5%');
    });

    it('should use abbreviated sess. label next to total count', () => {
        spectator.detectChanges();
        const totals = spectator.queryAll(byTestId('analytics-bar-engagement-sessions-total'));
        expect(totals[0]?.textContent?.replace(/\s+/g, ' ').trim()).toContain('sess.');
    });

    it('should use compact segment labels when counts are large', () => {
        spectator.setInput({
            data: [
                {
                    name: 'Chrome',
                    views: 320_000,
                    percentage: 50,
                    totalSessions: 640_000,
                    time: '1m'
                }
            ],
            status: ComponentStatus.LOADED
        });
        spectator.detectChanges();

        const engaged = spectator.query('.bar-engagement-row__segment--engaged span')?.textContent;
        expect(engaged).toBeTruthy();
        expect(engaged).not.toBe('320000');
        expect((engaged?.length ?? 0) < 12).toBe(true);
    });

    it('should resolve translated title when title input is a message key', () => {
        spectator.setInput({ title: 'analytics.engagement.charts.browser.title' });
        spectator.detectChanges();

        const card = spectator.query(byTestId('analytics-bar-engagement-chart'));
        expect(card?.querySelector('.p-card-title')?.textContent?.trim()).toBe(
            'analytics.engagement.charts.browser.title'
        );
    });
});
