import { addHours, endOfDay, format, startOfDay } from 'date-fns';

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    aggregateTotalConversions,
    createInitialRequestState,
    determineGranularityForTimeRange,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    getDateRange,
    transformDeviceBrowsersData,
    transformPageViewTimeLineData,
    transformTopPagesTableData
} from './analytics-data.utils';

import type {
    Granularity,
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
    TablePageData,
    TimeRange,
    TopPagePerformanceEntity,
    TopPerformanceTableEntity,
    TotalConversionsEntity,
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
                    'EventSummary.totalEvents': '1250'
                };

                const result = extractPageViews(mockData);
                expect(result).toBe(1250);
            });

            it('should return 0 when data is null', () => {
                const result = extractPageViews(null);
                expect(result).toBe(0);
            });

            it('should return 0 when totalRequest is missing', () => {
                const mockData: Partial<TotalPageViewsEntity> = {};

                const result = extractPageViews(mockData as TotalPageViewsEntity);
                expect(result).toBe(0);
            });

            it('should handle string numbers correctly', () => {
                const mockData: TotalPageViewsEntity = {
                    'EventSummary.totalEvents': '5000'
                };

                const result = extractPageViews(mockData);
                expect(result).toBe(5000);
            });
        });

        describe('extractSessions', () => {
            it('should extract sessions from valid data', () => {
                const mockData: UniqueVisitorsEntity = {
                    'EventSummary.uniqueVisitors': '342'
                };

                const result = extractSessions(mockData);
                expect(result).toBe(342);
            });

            it('should return 0 when data is null', () => {
                const result = extractSessions(null);
                expect(result).toBe(0);
            });

            it('should return NaN when totalUsers is missing', () => {
                const mockData: Partial<UniqueVisitorsEntity> = {};

                const result = extractSessions(mockData as UniqueVisitorsEntity);
                expect(result).toBeNaN();
            });
        });

        describe('extractTopPageValue', () => {
            it('should extract top page value from valid data', () => {
                const mockData: TopPagePerformanceEntity = {
                    'EventSummary.totalEvents': '890',
                    'EventSummary.title': 'Home Page',
                    'EventSummary.identifier': '/home'
                };

                const result = extractTopPageValue(mockData);
                expect(result).toBe(890);
            });

            it('should return 0 when data is null', () => {
                const result = extractTopPageValue(null);
                expect(result).toBe(0);
            });

            it('should return NaN when totalRequest is missing', () => {
                const mockData: Partial<TopPagePerformanceEntity> = {
                    'EventSummary.title': 'Home Page',
                    'EventSummary.identifier': '/home'
                };

                const result = extractTopPageValue(mockData as TopPagePerformanceEntity);
                expect(result).toBeNaN();
            });
        });

        describe('extractPageTitle', () => {
            it('should extract page title from valid data', () => {
                const mockData: TopPagePerformanceEntity = {
                    'EventSummary.totalEvents': '100',
                    'EventSummary.title': 'Home Page',
                    'EventSummary.identifier': '/home'
                };

                const result = extractPageTitle(mockData);
                expect(result).toBe('Home Page');
            });

            it('should return default message when data is null', () => {
                const result = extractPageTitle(null);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });

            it('should return default message when pageTitle is missing', () => {
                const mockData: Partial<TopPagePerformanceEntity> = {
                    'EventSummary.totalEvents': '100',
                    'EventSummary.identifier': '/home'
                };

                const result = extractPageTitle(mockData as TopPagePerformanceEntity);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });

            it('should return default message when pageTitle is empty', () => {
                const mockData: TopPagePerformanceEntity = {
                    'EventSummary.totalEvents': '100',
                    'EventSummary.title': '',
                    'EventSummary.identifier': '/home'
                };

                const result = extractPageTitle(mockData);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });
        });

        describe('aggregateTotalConversions', () => {
            it('should sum all totalEvents from multiple entities', () => {
                const mockEntities: TotalConversionsEntity[] = [
                    {
                        'EventSummary.totalEvents': '2'
                    },
                    {
                        'EventSummary.totalEvents': '1'
                    },
                    {
                        'EventSummary.totalEvents': '2'
                    }
                ];

                const result = aggregateTotalConversions(mockEntities);

                expect(result).toEqual({
                    'EventSummary.totalEvents': '5'
                });
            });

            it('should return null when array is empty', () => {
                const result = aggregateTotalConversions([]);

                expect(result).toBeNull();
            });

            it('should handle single entity', () => {
                const mockEntities: TotalConversionsEntity[] = [
                    {
                        'EventSummary.totalEvents': '10'
                    }
                ];

                const result = aggregateTotalConversions(mockEntities);

                expect(result).toEqual({
                    'EventSummary.totalEvents': '10'
                });
            });

            it('should handle entities with missing or zero values', () => {
                const mockEntities: TotalConversionsEntity[] = [
                    {
                        'EventSummary.totalEvents': '5'
                    },
                    {
                        'EventSummary.totalEvents': ''
                    },
                    {
                        'EventSummary.totalEvents': '0'
                    },
                    {
                        'EventSummary.totalEvents': '3'
                    }
                ];

                const result = aggregateTotalConversions(mockEntities);

                expect(result).toEqual({
                    'EventSummary.totalEvents': '8'
                });
            });

            it('should handle large numbers correctly', () => {
                const mockEntities: TotalConversionsEntity[] = [
                    {
                        'EventSummary.totalEvents': '1000'
                    },
                    {
                        'EventSummary.totalEvents': '2500'
                    },
                    {
                        'EventSummary.totalEvents': '500'
                    }
                ];

                const result = aggregateTotalConversions(mockEntities);

                expect(result).toEqual({
                    'EventSummary.totalEvents': '4000'
                });
            });
        });
    });

    describe('Transformation Functions', () => {
        describe('transformTopPagesTableData', () => {
            it('should transform valid table data correctly', () => {
                const mockData: TopPerformanceTableEntity[] = [
                    {
                        'EventSummary.title': 'Home Page',
                        'EventSummary.identifier': '/home',
                        'EventSummary.totalEvents': '1250'
                    },
                    {
                        'EventSummary.title': 'About Us',
                        'EventSummary.identifier': '/about',
                        'EventSummary.totalEvents': '890'
                    }
                ];

                const result = transformTopPagesTableData(mockData);
                const expected: TablePageData[] = [
                    {
                        pageTitle: 'Home Page',
                        path: '/home',
                        views: 1250
                    },
                    {
                        pageTitle: 'About Us',
                        path: '/about',
                        views: 890
                    }
                ];

                expect(result).toEqual(expected);
            });

            it('should return empty array when data is null', () => {
                const result = transformTopPagesTableData(null);
                expect(result).toEqual([]);
            });

            it('should return empty array when data is not an array', () => {
                const result = transformTopPagesTableData(
                    {} as unknown as TopPerformanceTableEntity[]
                );
                expect(result).toEqual([]);
            });

            it('should handle missing fields with defaults', () => {
                const mockData: Partial<TopPerformanceTableEntity>[] = [
                    {
                        'EventSummary.totalEvents': '500'
                    }
                ];

                const result = transformTopPagesTableData(mockData as TopPerformanceTableEntity[]);
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
                const mockData: PageViewTimeLineEntity[] = [
                    {
                        'EventSummary.day': '2023-12-01T00:00:00Z',
                        'EventSummary.day.day': '2023-12-01',
                        'EventSummary.totalEvents': '100'
                    },
                    {
                        'EventSummary.day': '2023-12-02T00:00:00Z',
                        'EventSummary.day.day': '2023-12-02',
                        'EventSummary.totalEvents': '150'
                    }
                ];

                const result = transformPageViewTimeLineData(mockData);

                expect(result.labels).toHaveLength(2);
                expect(result.datasets).toHaveLength(1);
                expect(result.datasets[0].data).toEqual([100, 150]);
                expect(result.datasets[0].label).toBe(
                    'analytics.charts.pageviews-timeline.dataset-label'
                );
                expect(result.datasets[0].borderColor).toBe('#3B82F6');
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
                const mockData: PageViewTimeLineEntity[] = [
                    {
                        'EventSummary.day': '2023-12-03T00:00:00Z',
                        'EventSummary.day.day': '2023-12-03',
                        'EventSummary.totalEvents': '200'
                    },
                    {
                        'EventSummary.day': '2023-12-01T00:00:00Z',
                        'EventSummary.day.day': '2023-12-01',
                        'EventSummary.totalEvents': '100'
                    },
                    {
                        'EventSummary.day': '2023-12-02T00:00:00Z',
                        'EventSummary.day.day': '2023-12-02',
                        'EventSummary.totalEvents': '150'
                    }
                ];

                const result = transformPageViewTimeLineData(mockData);

                // Should be sorted chronologically
                expect(result.datasets[0].data).toEqual([100, 150, 200]);
            });

            it('should handle missing totalRequest fields', () => {
                const mockData: Partial<PageViewTimeLineEntity>[] = [
                    {
                        'EventSummary.day': '2023-12-01T00:00:00Z',
                        'EventSummary.day.day': '2023-12-01'
                    }
                ];

                const result = transformPageViewTimeLineData(mockData as PageViewTimeLineEntity[]);

                expect(result.datasets[0].data).toEqual([0]);
            });

            describe('Date and Time Formatting', () => {
                it('should format labels as hours when all data is from the same day', () => {
                    // Use local dates to ensure same day detection works properly
                    const baseDate = new Date('2023-12-01T12:00:00'); // Local time, midday
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': new Date(
                                baseDate.getTime() - 3 * 60 * 60 * 1000
                            ).toISOString(), // 9 AM
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': new Date(
                                baseDate.getTime() + 2 * 60 * 60 * 1000
                            ).toISOString(), // 2 PM
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '150'
                        },
                        {
                            'EventSummary.day': new Date(
                                baseDate.getTime() + 6 * 60 * 60 * 1000
                            ).toISOString(), // 6 PM
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '200'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    // Should format as hours (HH:mm format) when all data is from same day
                    expect(result.labels).toHaveLength(3);
                    // Check that labels contain time format with HH:mm (24-hour format)
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^\d{1,2}:\d{2}$/);
                    });
                });

                it('should format labels as short date when data spans multiple days', () => {
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': '2023-12-01T12:00:00',
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': '2023-12-02T12:00:00',
                            'EventSummary.day.day': '2023-12-02',
                            'EventSummary.totalEvents': '150'
                        },
                        {
                            'EventSummary.day': '2023-12-03T12:00:00',
                            'EventSummary.day.day': '2023-12-03',
                            'EventSummary.totalEvents': '200'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    // Should format as day + month when data spans multiple days
                    expect(result.labels).toHaveLength(3);
                    // Check that labels contain date format (MMM dd)
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^[A-Za-z]{3}\s+\d{1,2}$/);
                    });
                });

                it('should handle same day detection correctly for edge cases', () => {
                    // Test data with same date but different times - use local time
                    const baseDate = new Date('2023-12-01T12:00:00');

                    const sameDayData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': startOfDay(baseDate).toISOString(), // startOfDay
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '50'
                        },
                        {
                            'EventSummary.day': endOfDay(baseDate).toISOString(), // endOfDay
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '75'
                        }
                    ];

                    const result = transformPageViewTimeLineData(sameDayData);

                    // Should still format as hours since it's the same day
                    expect(result.labels).toHaveLength(2);
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^\d{1,2}:\d{2}$/);
                    });
                });

                it('should handle data spanning just two different days', () => {
                    // Use dates that will definitely be different days even after timezone conversion
                    const twoDayData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': '2023-12-01T12:00:00.000', // Noon UTC - safe for most timezones
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': '2023-12-03T12:00:00.000', // Two days later at noon UTC
                            'EventSummary.day.day': '2023-12-03',
                            'EventSummary.totalEvents': '120'
                        }
                    ];

                    const result = transformPageViewTimeLineData(twoDayData);

                    // Should format as dates since data spans multiple days in any timezone
                    expect(result.labels).toHaveLength(2);
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        // Should use date format (MMM dd)
                        expect(label as string).toMatch(/^[A-Za-z]{3}\s+\d{1,2}$/);
                    });
                });

                it('should maintain chronological order when formatting hours', () => {
                    const baseDate = startOfDay(new Date('2023-12-01T12:00:00'));
                    const unorderedSameDayData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': addHours(baseDate, 7).toISOString(), // 7am
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '200'
                        },
                        {
                            'EventSummary.day': addHours(baseDate, 1).toISOString(), // 1am
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': addHours(baseDate, 13).toISOString(), // 3pm
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(unorderedSameDayData);

                    // Should be sorted chronologically: 1am, 7am, 3pm
                    expect(result.datasets[0].data).toEqual([100, 200, 150]);
                    expect(result.labels).toHaveLength(3);

                    // Verify hour format is used (HH:mm)
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^\d{1,2}:\d{2}$/);
                    });
                });

                it('should convert UTC dates to user local timezone for labels', () => {
                    // Mock UTC dates in the format that comes from the endpoint (without Z)
                    // These should be converted to user's local timezone
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': '2023-12-01T14:00:00.000', // 2 PM UTC (from endpoint format)
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': '2023-12-01T18:30:00.000', // 6:30 PM UTC (from endpoint format)
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    // The dates should be formatted using user's locale and timezone
                    // We can't predict the exact output since it depends on user's timezone,
                    // but we can verify the format is correct for local time
                    expect(result.labels).toHaveLength(2);
                    expect(result.datasets[0].data).toEqual([100, 150]);

                    // Check that labels are formatted as local time (HH:mm format)
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^\d{1,2}:\d{2}$/);
                    });
                });

                it('should handle dates across different days in local timezone', () => {
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': '2023-12-01T22:00:00.000', // 10 PM UTC (endpoint format)
                            'EventSummary.day.day': '2023-12-01',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': '2023-12-02T02:00:00.000', // 2 AM UTC next day (endpoint format)
                            'EventSummary.day.day': '2023-12-02',
                            'EventSummary.totalEvents': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    expect(result.labels).toHaveLength(2);
                    expect(result.datasets[0].data).toEqual([100, 150]);

                    // Should have date format since they're different days in local time
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        // Either time format (HH:mm) or date format (MMM dd) depending on timezone
                        expect(label as string).toMatch(
                            /^(\d{1,2}:\d{2})|([A-Za-z]{3}\s+\d{1,2})$/
                        );
                    });
                });

                it('should handle endpoint date format without Z suffix', () => {
                    // Test with the exact format that comes from the endpoint
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'EventSummary.day': '2025-08-05T16:00:00.000', // Endpoint format (no Z)
                            'EventSummary.day.day': '2025-08-05',
                            'EventSummary.totalEvents': '100'
                        },
                        {
                            'EventSummary.day': '2025-08-05T17:00:00.000', // Endpoint format (no Z)
                            'EventSummary.day.day': '2025-08-05',
                            'EventSummary.totalEvents': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    // Should parse correctly and convert to local timezone
                    expect(result.labels).toHaveLength(2);
                    expect(result.datasets[0].data).toEqual([100, 150]);

                    // Should format as time (same day) - HH:mm format
                    result.labels?.forEach((label) => {
                        expect(typeof label).toBe('string');
                        expect(label as string).toMatch(/^\d{1,2}:\d{2}$/);
                    });
                });
            });
        });

        describe('transformDeviceBrowsersData', () => {
            it('should transform valid device browsers data correctly', () => {
                const mockData: PageViewDeviceBrowsersEntity[] = [
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.count': '500'
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1',
                        'request.count': '300'
                    }
                ];

                const result = transformDeviceBrowsersData(mockData);

                expect(result.labels).toHaveLength(2);
                expect(result.datasets).toHaveLength(1);
                expect(result.datasets[0].data).toEqual([500, 300]);
                expect(result.datasets[0].label).toBe(
                    'analytics.charts.device-breakdown.dataset-label'
                );
                expect(result.datasets[0].backgroundColor).toHaveLength(2);
            });

            it('should return empty chart data when data is null', () => {
                const result = transformDeviceBrowsersData(null);

                expect(result.labels).toEqual([]);
                expect(result.datasets).toHaveLength(1);
                expect(result.datasets[0].data).toEqual([]);
                expect(result.datasets[0].backgroundColor).toEqual([]);
            });

            it('should return empty chart data when data is empty array', () => {
                const result = transformDeviceBrowsersData([]);

                expect(result.labels).toEqual([]);
                expect(result.datasets[0].data).toEqual([]);
            });

            it('should return "No Data" when no valid entries found', () => {
                const mockData: PageViewDeviceBrowsersEntity[] = [
                    {
                        'request.userAgent': 'Some browser',
                        'request.count': '0'
                    }
                ];

                const result = transformDeviceBrowsersData(mockData);

                expect(result.labels).toEqual(['No Data']);
                expect(result.datasets[0].data).toEqual([1]);
                expect(result.datasets[0].backgroundColor).toEqual(['#E5E7EB']);
            });

            it('should group by browser and device type correctly', () => {
                const mockData: PageViewDeviceBrowsersEntity[] = [
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.count': '200'
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.count': '300'
                    }
                ];

                const result = transformDeviceBrowsersData(mockData);

                // Should combine the two Chrome Desktop entries
                expect(result.labels).toHaveLength(1);
                expect(result.labels[0]).toContain('Chrome (Desktop)');
                expect(result.datasets[0].data).toEqual([500]); // 200 + 300
            });

            it('should sort results by usage descending', () => {
                const mockData: PageViewDeviceBrowsersEntity[] = [
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1',
                        'request.count': '100' // Less usage
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.count': '500' // More usage
                    }
                ];

                const result = transformDeviceBrowsersData(mockData);

                // Chrome Desktop should come first (higher usage)
                expect(result.labels[0]).toContain('Chrome (Desktop)');
                expect(result.labels[1]).toContain('Safari (Mobile)');
                expect(result.datasets[0].data).toEqual([500, 100]);
            });

            it('should handle invalid user agents gracefully', () => {
                const mockData: Partial<PageViewDeviceBrowsersEntity>[] = [
                    {
                        'request.userAgent': '',
                        'request.count': '100'
                    },
                    {
                        'request.count': '200'
                    }
                ];

                const result = transformDeviceBrowsersData(
                    mockData as PageViewDeviceBrowsersEntity[]
                );

                expect(result.labels).toEqual(['No Data']);
                expect(result.datasets[0].data).toEqual([1]);
            });
        });
    });

    describe('determineGranularityForTimeRange', () => {
        it('should return hour granularity for today', () => {
            const result = determineGranularityForTimeRange('today' as TimeRange);
            expect(result).toBe('hour' as Granularity);
        });

        it('should return hour granularity for yesterday', () => {
            const result = determineGranularityForTimeRange('yesterday' as TimeRange);
            expect(result).toBe('hour' as Granularity);
        });

        it('should return day granularity for last 7 days', () => {
            const result = determineGranularityForTimeRange('from 7 days ago to now' as TimeRange);
            expect(result).toBe('day' as Granularity);
        });

        it('should return day granularity for last 30 days', () => {
            const result = determineGranularityForTimeRange('from 30 days ago to now' as TimeRange);
            expect(result).toBe('day' as Granularity);
        });

        it('should return day granularity for CUSTOM_TIME_RANGE (default for custom ranges)', () => {
            const result = determineGranularityForTimeRange('CUSTOM_TIME_RANGE' as TimeRange);
            expect(result).toBe('day' as Granularity);
        });

        describe('all valid TimeRange values', () => {
            it('should handle all dropdown options correctly', () => {
                const testCases = [
                    { value: 'today', expected: 'hour' },
                    { value: 'yesterday', expected: 'hour' },
                    { value: 'from 7 days ago to now', expected: 'day' },
                    { value: 'from 30 days ago to now', expected: 'day' },
                    { value: 'CUSTOM_TIME_RANGE', expected: 'day' }
                ];

                testCases.forEach(({ value, expected }) => {
                    const result = determineGranularityForTimeRange(value as TimeRange);
                    expect(result).toBe(expected as Granularity);
                });
            });
        });

        describe('edge cases (fallback behavior)', () => {
            it('should return day granularity for unknown patterns', () => {
                // These would only occur in edge cases or custom implementations
                const edgeCases = ['invalid format', 'from custom date to custom date', ''];

                edgeCases.forEach((timeRange) => {
                    const result = determineGranularityForTimeRange(timeRange as TimeRange);
                    expect(result).toBe('day' as Granularity);
                });
            });
        });

        describe('custom date range', () => {
            it('should return day granularity for custom date range on the same month', () => {
                const result = determineGranularityForTimeRange(['2024-01-01', '2024-01-31']);
                expect(result).toBe('day');
            });

            it('should return hour granularity for custom date range on the same day', () => {
                const result = determineGranularityForTimeRange(['2024-01-01', '2024-01-01']);
                expect(result).toBe('hour');
            });

            it('should return month granularity for custom date range', () => {
                const result = determineGranularityForTimeRange(['2024-01-01', '2024-04-31']);
                expect(result).toBe('month');
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
            it('should return today range correctly', () => {
                const result = getDateRange('today');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 23:59:59');
            });

            it('should return yesterday range correctly', () => {
                const result = getDateRange('yesterday');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-14 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-14 23:59:59');
            });

            it('should return last 7 days range correctly', () => {
                const result = getDateRange('last7days');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-09 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 23:59:59');
            });

            it('should return last 30 days range correctly', () => {
                const result = getDateRange('last30days');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-17 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-01-15 23:59:59');
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

            it('should handle leap year dates correctly', () => {
                jest.setSystemTime(new Date('2024-02-15T12:00:00.000Z'));

                const result = getDateRange('yesterday');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-02-14 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2024-02-14 23:59:59');
            });

            it('should handle month boundary dates correctly', () => {
                jest.setSystemTime(new Date('2024-01-01T12:00:00.000Z'));

                const result = getDateRange('yesterday');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-31 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-31 23:59:59');
            });

            it('should handle year boundary dates correctly', () => {
                jest.setSystemTime(new Date('2024-01-01T12:00:00.000Z'));

                const result = getDateRange('yesterday');
                const [startDate, endDate] = result;

                expect(format(startDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-31 00:00:00');
                expect(format(endDate, 'yyyy-MM-dd HH:mm:ss')).toEqual('2023-12-31 23:59:59');
            });
        });
    });
});
