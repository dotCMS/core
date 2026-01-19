import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotAnalyticsPlatformsTableComponent,
    PlatformsData
} from './dot-analytics-platforms-table.component';

describe('DotAnalyticsPlatformsTableComponent', () => {
    let spectator: Spectator<DotAnalyticsPlatformsTableComponent>;

    const mockPlatformsData: PlatformsData = {
        device: [
            { name: 'Desktop', views: 77053, percentage: 80, time: '2m 45s' },
            { name: 'Mobile', views: 16071, percentage: 17, time: '1m 47s' },
            { name: 'Tablet', views: 2531, percentage: 3, time: '2m 00s' }
        ],
        browser: [
            { name: 'Chrome', views: 55000, percentage: 57, time: '2m 30s' },
            { name: 'Safari', views: 25000, percentage: 26, time: '2m 15s' },
            { name: 'Firefox', views: 15655, percentage: 17, time: '2m 00s' }
        ],
        language: [
            { name: 'English', views: 70000, percentage: 73, time: '2m 40s' },
            { name: 'Spanish', views: 20000, percentage: 21, time: '2m 20s' },
            { name: 'French', views: 5655, percentage: 6, time: '1m 50s' }
        ]
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsPlatformsTableComponent,
        imports: [DotMessagePipe],
        providers: []
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                platforms: mockPlatformsData,
                status: ComponentStatus.LOADED
            } as unknown,
            detectChanges: false
        });
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should display p-card', () => {
            spectator.detectChanges();
            expect(spectator.query('p-card')).toExist();
        });

        it('should display p-tabView with 3 tabs', () => {
            spectator.detectChanges();
            expect(spectator.query('p-tabView')).toExist();
            expect(spectator.queryAll('p-tabPanel').length).toBe(3);
        });
    });

    describe('Loading State', () => {
        it('should show skeleton when status is LOADING', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query('.skeleton-table')).toExist();
        });

        it('should show skeleton when status is INIT', () => {
            spectator.setInput('status', ComponentStatus.INIT);
            spectator.detectChanges();

            expect(spectator.query('.skeleton-table')).toExist();
        });

        it('should show table when status is LOADED', () => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('p-table')).toExist();
            expect(spectator.query('.skeleton-table')).not.toExist();
        });
    });

    describe('Data Display', () => {
        it('should display device data correctly', () => {
            spectator.detectChanges();
            const deviceData = spectator.component.$deviceData();
            expect(deviceData.length).toBe(3);
            expect(deviceData[0].name).toBe('Desktop');
        });

        it('should display browser data correctly', () => {
            spectator.detectChanges();
            const browserData = spectator.component.$browserData();
            expect(browserData.length).toBe(3);
            expect(browserData[0].name).toBe('Chrome');
        });

        it('should display language data correctly', () => {
            spectator.detectChanges();
            const languageData = spectator.component.$languageData();
            expect(languageData.length).toBe(3);
            expect(languageData[0].name).toBe('English');
        });
    });
});
