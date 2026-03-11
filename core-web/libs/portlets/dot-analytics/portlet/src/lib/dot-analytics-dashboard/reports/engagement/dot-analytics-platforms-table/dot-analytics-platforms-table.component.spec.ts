import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
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
            { name: 'en', views: 60000, percentage: 63, time: '2m 40s' },
            { name: 'es', views: 25000, percentage: 26, time: '2m 10s' },
            { name: 'fr', views: 10655, percentage: 11, time: '1m 55s' }
        ]
    };

    const createComponent = createComponentFactory({
        component: DotAnalyticsPlatformsTableComponent,
        imports: [DotMessagePipe],
        providers: [mockProvider(DotMessageService, { get: jest.fn((key: string) => key) })]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        spectator.setInput('platforms', mockPlatformsData);
        spectator.setInput('status', ComponentStatus.LOADED);
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

        it('should display p-tabs with 3 tab panels', () => {
            spectator.detectChanges();
            expect(spectator.query('p-tabs')).toExist();
            expect(spectator.queryAll('p-tabpanel').length).toBe(3);
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
        it('should compute device data from platforms input', () => {
            spectator.detectChanges();
            const deviceData = spectator.component.$deviceData();
            expect(deviceData.length).toBe(3);
            expect(deviceData[0].name).toBe('Desktop');
        });

        it('should compute browser data from platforms input', () => {
            spectator.detectChanges();
            const browserData = spectator.component.$browserData();
            expect(browserData.length).toBe(3);
            expect(browserData[0].name).toBe('Chrome');
        });

        it('should compute language data from platforms input', () => {
            spectator.detectChanges();
            const languageData = spectator.component.$languageData();
            expect(languageData.length).toBe(3);
            expect(languageData[0].name).toBe('en');
        });

        it('should return empty arrays when platforms is null', () => {
            spectator.setInput('platforms', null);
            spectator.detectChanges();

            expect(spectator.component.$deviceData()).toEqual([]);
            expect(spectator.component.$browserData()).toEqual([]);
            expect(spectator.component.$languageData()).toEqual([]);
        });
    });

    describe('Empty State', () => {
        it('should show full empty state when all platform data is empty and status is LOADED', () => {
            spectator.setInput('platforms', { device: [], browser: [], language: [] });
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="platforms-empty-all"]')).toExist();
            expect(spectator.query('p-tabs')).not.toExist();
        });

        it('should not show full empty state when at least one tab has data', () => {
            spectator.setInput('platforms', {
                device: [],
                browser: [],
                language: [{ name: 'en', views: 100, percentage: 100, time: '1m 0s' }]
            });
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="platforms-empty-all"]')).not.toExist();
            expect(spectator.query('p-tabs')).toExist();
        });

        it('should not show full empty state while loading', () => {
            spectator.setInput('platforms', { device: [], browser: [], language: [] });
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="platforms-empty-all"]')).not.toExist();
        });
    });
});
