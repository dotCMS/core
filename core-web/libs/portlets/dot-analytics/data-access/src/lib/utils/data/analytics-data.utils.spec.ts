import { format, parse } from 'date-fns';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    createEmptyAnalyticsEntity,
    createEmptyTrafficVsConversionsEntity,
    createInitialRequestState,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    fillMissingApiDates,
    fillMissingDates,
    getDateRange,
    getPreviousPeriod,
    transformBrowserBreakdownToPieChartEntries,
    transformDeviceBreakdownToPieChartEntries,
    transformPageViewTimeLineData,
    transformTopPagesTableData,
    transformConversionTrendData,
    transformTrafficVsConversionsData
} from './analytics-data.utils';

import { AnalyticsChartColors } from '../../constants';
import { Granularity } from '../../types';

// eslint-disable-next-line no-duplicate-imports
import type { ConversionTrendEntity } from './analytics-data.utils';
// eslint-disable-next-line no-duplicate-imports
import type {
    BrowserBreakdownData,
    DeviceBreakdownData,
    TablePageData,
    TopContentData,
    TopPagePerformanceEntity,
    TotalEventsByDayData,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';

describe('Analytics Data Utils', () => {
    describe('createInitialRequestState', () => {
        it('should create an initial request state with INIT status', () => {
            const result = createInitialRequestState();

            expect(result).toEqual({
                status: ComponentStatus.INIT,
                data: null,
                error: null
            });
        });

        it('should create a typed initial request state', () => {
            interface TestData {
                id: number;
                name: string;
            }

            const result = createInitialRequestState<TestData>();

            expect(result.status).toBe(ComponentStatus.INIT);
            expect(result.data).toBeNull();
            expect(result.error).toBeNull();
        });
    });

    describe('Extraction Functions', () => {
        describe('extractPageViews', () => {
            it('should extract page views from valid data', () => {
                const mockData: TotalPageViewsEntity = {
                    totalEvents: 1250
                };

                const result = extractPageViews(mockData);
                expect(result).toBe(1250);
            });

            it('should return null when data is null', () => {
                const result = extractPageViews(null);
                expect(result).toBeNull();
            });

            it('should return null when totalEvents is zero', () => {
                const mockData: TotalPageViewsEntity = {
                    totalEvents: 0
                };

                const result = extractPageViews(mockData);
                expect(result).toBeNull();
            });

            it('should handle numeric values correctly', () => {
                const mockData: TotalPageViewsEntity = {
                    totalEvents: 5000
                };

                const result = extractPageViews(mockData);
                expect(result).toBe(5000);
            });
        });

        describe('extractSessions', () => {
            it('should extract sessions from valid data', () => {
                const mockData: UniqueVisitorsEntity = {
                    uniqueVisitors: 342
                };

                const result = extractSessions(mockData);
                expect(result).toBe(342);
            });

            it('should return null when data is null', () => {
                const result = extractSessions(null);
                expect(result).toBeNull();
            });

            it('should return null when uniqueVisitors is zero', () => {
                const mockData: UniqueVisitorsEntity = {
                    uniqueVisitors: 0
                };

                const result = extractSessions(mockData as UniqueVisitorsEntity);
                expect(result).toBeNull();
            });
        });

        describe('extractTopPageValue', () => {
            it('should extract top page value from valid data', () => {
                const mockData: TopPagePerformanceEntity = {
                    totalEvents: 890,
                    title: 'Home Page',
                    identifier: '/home'
                };

                const result = extractTopPageValue(mockData);
                expect(result).toBe(890);
            });

            it('should return null when data is null', () => {
                const result = extractTopPageValue(null);
                expect(result).toBeNull();
            });

            it('should return null when totalEvents is zero', () => {
                const mockData: TopPagePerformanceEntity = {
                    totalEvents: 0,
                    title: 'Home Page',
                    identifier: '/home'
                };

                const result = extractTopPageValue(mockData);
                expect(result).toBeNull();
            });
        });

        describe('extractPageTitle', () => {
            it('should extract page title from valid data', () => {
                const mockData: TopPagePerformanceEntity = {
                    totalEvents: 100,
                    title: 'Home Page',
                    identifier: '/home'
                };

                const result = extractPageTitle(mockData);
                expect(result).toBe('Home Page');
            });

            it('should return default message when data is null', () => {
                const result = extractPageTitle(null);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });

            it('should return default message when title is missing', () => {
                const mockData: Partial<TopPagePerformanceEntity> = {
                    totalEvents: 100,
                    identifier: '/home'
                };

                const result = extractPageTitle(mockData as TopPagePerformanceEntity);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });

            it('should return default message when title is empty', () => {
                const mockData: TopPagePerformanceEntity = {
                    totalEvents: 100,
                    title: '',
                    identifier: '/home'
                };

                const result = extractPageTitle(mockData);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });
        });
    });

    describe('Transformation Functions', () => {
        describe('transformTopPagesTableData', () => {
            it('should transform valid table data correctly', () => {
                const mockData: TopContentData[] = [
                    { title: 'Home Page', identifier: '/home', totalEvents: 1250 },
                    { title: 'About Us', identifier: '/about', totalEvents: 890 }
                ];

                const result = transformTopPagesTableData(mockData);
                const expected: TablePageData[] = [
                    { pageTitle: 'Home Page', path: '/home', views: 1250 },
                    { pageTitle: 'About Us', path: '/about', views: 890 }
                ];

                expect(result).toEqual(expected);
            });

            it('should return empty array when data is null', () => {
                const result = transformTopPagesTableData(null);
                expect(result).toEqual([]);
            });

            it('should return empty array when data is not an array', () => {
                const result = transformTopPagesTableData({} as unknown as TopContentData[]);
                expect(result).toEqual([]);
            });

            it('should handle missing fields with defaults', () => {
                const mockData: Partial<TopContentData>[] = [{ totalEvents: 500 }];

                const result = transformTopPagesTableData(mockData as TopContentData[]);
                const expected: TablePageData[] = [
                    {
                        pageTitle: 'analytics.table.data.not-available',
                        path: 'analytics.table.data.not-available',
                        views: 500
                    }
                ];

                expect(result).toEqual(expected);
            });

            it('should handle empty array', () => {
                const result = transformTopPagesTableData([]);
                expect(result).toEqual([]);
            });
        });

        describe('transformPageViewTimeLineData', () => {
            it('should transform valid timeline data correctly', () => {
                const mockData: TotalEventsByDayData[] = [
                    { day: '2023-12-01', totalEvents: 100 },
                    { day: '2023-12-02', totalEvents: 150 }
                ];

                const result = transformPageViewTimeLineData(mockData);

                expect(result.labels).toHaveLength(2);
                expect(result.datasets).toHaveLength(1);
                expect(result.datasets[0].data).toEqual([100, 150]);
                expect(result.datasets[0].label).toBe(
                    'analytics.charts.pageviews-timeline.dataset-label'
                );
                expect(result.datasets[0].borderColor).toBe(AnalyticsChartColors.primary.line);
                expect(result.datasets[0].cubicInterpolationMode).toBe('monotone');
            });

            it('should return empty chart data when data is null', () => {
                const result = transformPageViewTimeLineData(null);

                expect(result.labels).toEqual([]);
                expect(result.datasets).toHaveLength(1);
                expect(result.datasets[0].data).toEqual([]);
                expect(result.datasets[0].label).toBe(
                    'analytics.charts.pageviews-timeline.dataset-label'
                );
            });

            it('should return empty chart data when data is empty array', () => {
                const result = transformPageViewTimeLineData([]);

                expect(result.labels).toEqual([]);
                expect(result.datasets[0].data).toEqual([]);
            });

            it('should sort data by date correctly', () => {
                const mockData: TotalEventsByDayData[] = [
                    { day: '2023-12-03', totalEvents: 200 },
                    { day: '2023-12-01', totalEvents: 100 },
                    { day: '2023-12-02', totalEvents: 150 }
                ];

                const result = transformPageViewTimeLineData(mockData);

                expect(result.datasets[0].data).toEqual([100, 150, 200]);
            });

            it('should handle zero totalEvents', () => {
                const mockData: TotalEventsByDayData[] = [{ day: '2023-12-01', totalEvents: 0 }];

                const result = transformPageViewTimeLineData(mockData);

                expect(result.datasets[0].data).toEqual([0]);
            });

            describe('Date and Time Formatting', () => {
                it('should format labels as short date when data spans multiple days', () => {
                    const mockData: TotalEventsByDayData[] = [
                        { day: '2023-12-01', totalEvents: 100 },
                        { day: '2023-12-02', totalEvents: 150 },
                        { day: '2023-12-03', totalEvents: 200 }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    expect(result.labels).toHaveLength(3);
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^[A-Za-z]{3}\s+\d{1,2}$/);
                    });
                });

                it('should format labels as hours when all data is from the same day', () => {
                    const mockData: TotalEventsByDayData[] = [
                        { day: '2023-12-01', totalEvents: 100 }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    expect(result.labels).toHaveLength(1);
                    expect(result.datasets[0].data).toEqual([100]);
                });

                it('should handle data spanning two different days', () => {
                    const mockData: TotalEventsByDayData[] = [
                        { day: '2023-12-01', totalEvents: 100 },
                        { day: '2023-12-03', totalEvents: 120 }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    expect(result.labels).toHaveLength(2);
                    expect(result.datasets[0].data).toEqual([100, 120]);
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^[A-Za-z]{3}\s+\d{1,2}$/);
                    });
                });

                it('should maintain chronological order', () => {
                    const mockData: TotalEventsByDayData[] = [
                        { day: '2023-12-07', totalEvents: 200 },
                        { day: '2023-12-01', totalEvents: 100 },
                        { day: '2023-12-04', totalEvents: 150 }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    expect(result.datasets[0].data).toEqual([100, 150, 200]);
                    expect(result.labels).toHaveLength(3);
                });
            });

            describe('API date-only calendar labels (regression)', () => {
                const originalTz = process.env.TZ;

                beforeAll(() => {
                    process.env.TZ = 'America/New_York';
                });

                afterAll(() => {
                    if (originalTz === undefined) {
                        delete process.env.TZ;
                    } else {
                        process.env.TZ = originalTz;
                    }
                });

                /**
                 * Node parses `new Date("yyyy-MM-dd")` as UTC; in US timezones that shifts the calendar day
                 * when formatting. Labels must match local-calendar parse of the API string.
                 * Use two buckets so labels use day format (MMM dd), not same-day hourly format.
                 */
                it('should match date-fns local parse for yyyy-MM-dd, not UTC Date parse', () => {
                    const apiDayPrev = '2026-05-04';
                    const apiDay = '2026-05-05';
                    const result = transformPageViewTimeLineData([
                        { day: apiDayPrev, totalEvents: 1 },
                        { day: apiDay, totalEvents: 7 }
                    ]);
                    const expectedLabel = format(parse(apiDay, 'yyyy-MM-dd', new Date()), 'MMM dd');
                    expect(result.labels?.[1]).toBe(expectedLabel);
                });
            });
        });

        describe('transformConversionTrendData', () => {
            describe('API date-only calendar labels (regression)', () => {
                const originalTz = process.env.TZ;

                beforeAll(() => {
                    process.env.TZ = 'America/New_York';
                });

                afterAll(() => {
                    if (originalTz === undefined) {
                        delete process.env.TZ;
                    } else {
                        process.env.TZ = originalTz;
                    }
                });

                it('should match date-fns local parse for yyyy-MM-dd, not UTC Date parse', () => {
                    const apiDayPrev = '2026-05-04';
                    const apiDay = '2026-05-05';
                    const result = transformConversionTrendData([
                        { day: apiDayPrev, totalEvents: 1 },
                        { day: apiDay, totalEvents: 7 }
                    ]);
                    const expectedLabel = format(parse(apiDay, 'yyyy-MM-dd', new Date()), 'MMM dd');
                    expect(result.labels?.[1]).toBe(expectedLabel);
                });
            });
        });

        describe('transformTrafficVsConversionsData', () => {
            describe('API date-only calendar labels (regression)', () => {
                const originalTz = process.env.TZ;

                beforeAll(() => {
                    process.env.TZ = 'America/New_York';
                });

                afterAll(() => {
                    if (originalTz === undefined) {
                        delete process.env.TZ;
                    } else {
                        process.env.TZ = originalTz;
                    }
                });

                it('should match date-fns local parse for yyyy-MM-dd, not UTC Date parse', () => {
                    const apiDayPrev = '2026-05-04';
                    const apiDay = '2026-05-05';
                    const result = transformTrafficVsConversionsData([
                        {
                            day: apiDayPrev,
                            uniqueVisitors: 10,
                            uniqueConvertingVisitors: 1
                        },
                        {
                            day: apiDay,
                            uniqueVisitors: 20,
                            uniqueConvertingVisitors: 3
                        }
                    ]);
                    const expectedLabel = format(parse(apiDay, 'yyyy-MM-dd', new Date()), 'MMM dd');
                    expect(result.labels?.[1]).toBe(expectedLabel);
                });
            });
        });

        describe('transformBrowserBreakdownToPieChartEntries', () => {
            it('should map pre-aggregated browser totals and sort descending', () => {
                const mockData: BrowserBreakdownData[] = [
                    { browser: 'Chrome', total: 700 },
                    { browser: 'Safari', total: 300 }
                ];

                const result = transformBrowserBreakdownToPieChartEntries(mockData);

                expect(result).toEqual([
                    { name: 'Chrome', value: 700 },
                    { name: 'Safari', value: 300 }
                ]);
            });

            it('should return empty array when data is null or empty', () => {
                expect(transformBrowserBreakdownToPieChartEntries(null)).toEqual([]);
                expect(transformBrowserBreakdownToPieChartEntries([])).toEqual([]);
            });

            it('should sort by total descending when input order varies', () => {
                const mockData: BrowserBreakdownData[] = [
                    { browser: 'Firefox', total: 100 },
                    { browser: 'Chrome', total: 300 },
                    { browser: 'Safari', total: 200 }
                ];

                const result = transformBrowserBreakdownToPieChartEntries(mockData);

                expect(result[0]).toEqual({ name: 'Chrome', value: 300 });
                expect(result[1]).toEqual({ name: 'Safari', value: 200 });
                expect(result[2]).toEqual({ name: 'Firefox', value: 100 });
            });

            it('should cap results at 10 entries when otherLabel is omitted', () => {
                const mockData: BrowserBreakdownData[] = Array.from({ length: 15 }, (_, i) => ({
                    browser: `Browser${i}`,
                    total: 100 - i
                }));

                const result = transformBrowserBreakdownToPieChartEntries(mockData);

                expect(result).toHaveLength(10);
                expect(result[0].value).toBe(100);
                expect(result.every((e) => e.name !== 'Other')).toBe(true);
            });

            it('should aggregate overflow browsers into Other when otherLabel is set', () => {
                const mockData: BrowserBreakdownData[] = Array.from({ length: 15 }, (_, i) => ({
                    browser: `Browser${i}`,
                    total: 100 - i
                }));

                const result = transformBrowserBreakdownToPieChartEntries(mockData, {
                    otherLabel: 'Other'
                });

                expect(result).toHaveLength(10);
                expect(result[8]).toMatchObject({ name: 'Browser8', value: 92 });
                expect(result[9]).toEqual({ name: 'Other', value: 531 });
            });

            it('should not add Other when distinct browsers count equals maxSlices', () => {
                const mockData: BrowserBreakdownData[] = Array.from({ length: 10 }, (_, i) => ({
                    browser: `Browser${i}`,
                    total: 100 - i
                }));

                const result = transformBrowserBreakdownToPieChartEntries(mockData, {
                    otherLabel: 'Other'
                });

                expect(result).toHaveLength(10);
                expect(result.every((e) => e.name !== 'Other')).toBe(true);
            });
        });

        describe('transformDeviceBreakdownToPieChartEntries', () => {
            it('should map pre-aggregated device totals and sort descending', () => {
                const mockData: DeviceBreakdownData[] = [
                    { device: 'Desktop', total: 600 },
                    { device: 'Mobile', total: 300 }
                ];

                const result = transformDeviceBreakdownToPieChartEntries(mockData);

                expect(result).toEqual([
                    { name: 'Desktop', value: 600 },
                    { name: 'Mobile', value: 300 }
                ]);
            });

            it('should return empty array when data is null or empty', () => {
                expect(transformDeviceBreakdownToPieChartEntries(null)).toEqual([]);
                expect(transformDeviceBreakdownToPieChartEntries([])).toEqual([]);
            });

            it('should sort by total descending', () => {
                const mockData: DeviceBreakdownData[] = [
                    { device: 'Mobile', total: 200 },
                    { device: 'Desktop', total: 700 },
                    { device: 'Tablet', total: 50 }
                ];

                const result = transformDeviceBreakdownToPieChartEntries(mockData);

                expect(result[0]).toEqual({ name: 'Desktop', value: 700 });
                expect(result[1]).toEqual({ name: 'Mobile', value: 200 });
                expect(result[2]).toEqual({ name: 'Tablet', value: 50 });
            });

            it('should aggregate overflow devices into Other when otherLabel is set', () => {
                const mockData: DeviceBreakdownData[] = Array.from({ length: 15 }, (_, i) => ({
                    device: `Device${i}`,
                    total: 100 - i
                }));

                const result = transformDeviceBreakdownToPieChartEntries(mockData, {
                    otherLabel: 'Other'
                });

                expect(result).toHaveLength(10);
                expect(result[9]).toEqual({ name: 'Other', value: 531 });
            });
        });
    });

    describe('getDateRange', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            jest.setSystemTime(new Date('2024-01-15T04:00:00.000'));
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        describe('success cases', () => {
            it('should return last 7 complete days ending yesterday', () => {
                const result = getDateRange('last7days');
                const [startDate, endDate] = result;

                // Today (2024-01-15) is excluded; range is the 7 complete days ending yesterday
                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-08 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-14 23:59:59');
            });

            it('should return last 30 complete days ending yesterday', () => {
                const result = getDateRange('last30days');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-16 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-14 23:59:59');
            });

            it('should handle custom date range array correctly', () => {
                const result = getDateRange(['2024-01-01', '2024-01-31']);
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-01 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-31 23:59:59');
            });

            it('should handle single day custom range correctly', () => {
                const result = getDateRange(['2024-01-15', '2024-01-15']);
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 23:59:59');
            });
        });
    });

    describe('fillMissingApiDates + transformPageViewTimeLineData (relative range regression)', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            // Reproduces the reported bug: today = Jun 10, API returns Jun 03 … Jun 09.
            jest.setSystemTime(new Date('2026-06-10T12:00:00.000'));
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('keeps the earliest day and does not append today for a last7days range', () => {
            // Sparse API rows for the 7 complete days ending yesterday (Jun 03 … Jun 09).
            const apiData: TotalEventsByDayData[] = [
                { day: '2026-06-03', totalEvents: 993 },
                { day: '2026-06-04', totalEvents: 1136 },
                { day: '2026-06-05', totalEvents: 1558 },
                { day: '2026-06-06', totalEvents: 718 },
                { day: '2026-06-07', totalEvents: 721 },
                { day: '2026-06-08', totalEvents: 1107 },
                { day: '2026-06-09', totalEvents: 2424 }
            ];

            const filled = fillMissingApiDates(apiData, 'last7days', 'day', (date) => ({
                day: format(date, 'yyyy-MM-dd'),
                totalEvents: 0
            }));

            // Window must match the API data exactly: Jun 03 … Jun 09 — no Jun 10 (today) bucket.
            expect(filled.map((item) => item.day)).toEqual([
                '2026-06-03',
                '2026-06-04',
                '2026-06-05',
                '2026-06-06',
                '2026-06-07',
                '2026-06-08',
                '2026-06-09'
            ]);
            // Earliest real day is retained with its value (previously dropped).
            expect(filled[0]).toEqual({ day: '2026-06-03', totalEvents: 993 });

            const chart = transformPageViewTimeLineData(filled);
            expect(chart.labels?.[0]).toBe('Jun 03');
            expect(chart.labels?.[chart.labels.length - 1]).toBe('Jun 09');
            expect(chart.datasets[0].data).toEqual([993, 1136, 1558, 718, 721, 1107, 2424]);
        });
    });

    describe('getPreviousPeriod', () => {
        it('should return previous period of same length for custom date range', () => {
            const result = getPreviousPeriod(['2026-02-01', '2026-02-06']);
            expect(result).toEqual(['2026-01-26', '2026-01-31']);
        });

        it('should return previous period for single-day custom range', () => {
            const result = getPreviousPeriod(['2026-02-01', '2026-02-01']);
            expect(result).toEqual(['2026-01-31', '2026-01-31']);
        });

        it('should return previous period for predefined last7days', () => {
            jest.useFakeTimers();
            jest.setSystemTime(new Date('2024-01-15T12:00:00.000Z'));
            const result = getPreviousPeriod('last7days');
            jest.useRealTimers();
            // last7days: Jan 8 - Jan 14 (7 complete days ending yesterday). Previous: Jan 1 - Jan 7
            expect(result[0]).toBe('2024-01-01');
            expect(result[1]).toBe('2024-01-07');
        });
    });

    describe('fillMissingDates', () => {
        describe('with ConversionTrendEntity', () => {
            it('should return empty array when data is null', () => {
                const result = fillMissingDates(
                    null as unknown as ConversionTrendEntity[],
                    ['2024-01-01', '2024-01-03'],
                    Granularity.DAY,
                    createEmptyAnalyticsEntity
                );

                expect(result).toEqual([]);
            });

            it('should return empty array when data is not an array', () => {
                const result = fillMissingDates(
                    {} as unknown as ConversionTrendEntity[],
                    ['2024-01-01', '2024-01-03'],
                    Granularity.DAY,
                    createEmptyAnalyticsEntity
                );

                expect(result).toEqual([]);
            });

            it('should fill all dates in range when data is empty', () => {
                const result = fillMissingDates<ConversionTrendEntity>(
                    [],
                    ['2024-01-01', '2024-01-03'],
                    Granularity.DAY,
                    createEmptyAnalyticsEntity
                );

                // Should create 3 days worth of empty data
                expect(result).toHaveLength(3);
                result.forEach((item) => {
                    expect(item['EventSummary.totalEvents']).toBe('0');
                });
            });

            it('should return correct number of entries for date range', () => {
                const result = fillMissingDates<ConversionTrendEntity>(
                    [],
                    ['2024-01-01', '2024-01-05'],
                    Granularity.DAY,
                    createEmptyAnalyticsEntity
                );

                // 5 days: Jan 1, 2, 3, 4, 5
                expect(result).toHaveLength(5);
                // All should be zero since input was empty
                result.forEach((item) => {
                    expect(item['EventSummary.totalEvents']).toBe('0');
                    expect(item['EventSummary.day']).toBeDefined();
                    expect(item['EventSummary.day.day']).toBeDefined();
                });
            });

            it('should use the factory function to create empty entities', () => {
                const customFactory = jest.fn((date: Date, dateKey: string) => ({
                    'EventSummary.day': dateKey,
                    'EventSummary.day.day': format(date, 'yyyy-MM-dd'),
                    'EventSummary.totalEvents': '999' // Custom value to verify factory is used
                }));

                const result = fillMissingDates(
                    [],
                    ['2024-01-01', '2024-01-02'],
                    Granularity.DAY,
                    customFactory
                );

                expect(customFactory).toHaveBeenCalledTimes(2);
                expect(result[0]['EventSummary.totalEvents']).toBe('999');
            });
        });

        describe('with TrafficVsConversionsEntity', () => {
            it('should create entities with correct structure when filling gaps', () => {
                const result = fillMissingDates(
                    [],
                    ['2024-01-01', '2024-01-02'],
                    Granularity.DAY,
                    createEmptyTrafficVsConversionsEntity
                );

                expect(result).toHaveLength(2);
                result.forEach((item) => {
                    expect(item['EventSummary.uniqueVisitors']).toBe('0');
                    expect(item['EventSummary.uniqueConvertingVisitors']).toBe('0');
                    expect(item['EventSummary.day']).toBeDefined();
                    expect(item['EventSummary.day.day']).toBeDefined();
                });
            });
        });
    });

    describe('Entity Factories', () => {
        const testDate = new Date('2024-01-15T12:00:00.000Z');
        const testDateKey = testDate.toISOString();

        describe('createEmptyAnalyticsEntity', () => {
            it('should create entity with correct structure', () => {
                const result = createEmptyAnalyticsEntity<ConversionTrendEntity>(
                    testDate,
                    testDateKey
                );

                expect(result).toEqual({
                    'EventSummary.day': testDateKey,
                    'EventSummary.day.day': '2024-01-15',
                    'EventSummary.totalEvents': '0'
                });
            });
        });

        describe('createEmptyTrafficVsConversionsEntity', () => {
            it('should create entity with correct structure', () => {
                const result = createEmptyTrafficVsConversionsEntity(testDate, testDateKey);

                expect(result).toEqual({
                    'EventSummary.day': testDateKey,
                    'EventSummary.day.day': '2024-01-15',
                    'EventSummary.uniqueVisitors': '0',
                    'EventSummary.uniqueConvertingVisitors': '0'
                });
            });
        });
    });
});
