import {
    determineGranularityForTimeRange,
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
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
    TopPerformaceTableEntity,
    TotalPageViewsEntity,
    UniqueVisitorsEntity
} from '../../types';

describe('Analytics Data Utils', () => {
    describe('Extraction Functions', () => {
        describe('extractPageViews', () => {
            it('should extract page views from valid data', () => {
                const mockData: TotalPageViewsEntity = {
                    'request.totalRequest': '1250'
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
                    'request.totalRequest': '5000'
                };

                const result = extractPageViews(mockData);
                expect(result).toBe(5000);
            });
        });

        describe('extractSessions', () => {
            it('should extract sessions from valid data', () => {
                const mockData: UniqueVisitorsEntity = {
                    'request.totalUsers': '342'
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
                    'request.totalRequest': '890',
                    'request.pageTitle': 'Home Page',
                    'request.path': '/home'
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
                    'request.pageTitle': 'Home Page',
                    'request.path': '/home'
                };

                const result = extractTopPageValue(mockData as TopPagePerformanceEntity);
                expect(result).toBeNaN();
            });
        });

        describe('extractPageTitle', () => {
            it('should extract page title from valid data', () => {
                const mockData: TopPagePerformanceEntity = {
                    'request.totalRequest': '100',
                    'request.pageTitle': 'Home Page',
                    'request.path': '/home'
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
                    'request.totalRequest': '100',
                    'request.path': '/home'
                };

                const result = extractPageTitle(mockData as TopPagePerformanceEntity);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });

            it('should return default message when pageTitle is empty', () => {
                const mockData: TopPagePerformanceEntity = {
                    'request.totalRequest': '100',
                    'request.pageTitle': '',
                    'request.path': '/home'
                };

                const result = extractPageTitle(mockData);
                expect(result).toBe('analytics.metrics.pageTitle.not-available');
            });
        });
    });

    describe('Transformation Functions', () => {
        describe('transformTopPagesTableData', () => {
            it('should transform valid table data correctly', () => {
                const mockData: TopPerformaceTableEntity[] = [
                    {
                        'request.pageTitle': 'Home Page',
                        'request.path': '/home',
                        'request.totalRequest': '1250'
                    },
                    {
                        'request.pageTitle': 'About Us',
                        'request.path': '/about',
                        'request.totalRequest': '890'
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
                    {} as unknown as TopPerformaceTableEntity[]
                );
                expect(result).toEqual([]);
            });

            it('should handle missing fields with defaults', () => {
                const mockData: Partial<TopPerformaceTableEntity>[] = [
                    {
                        'request.totalRequest': '500'
                    }
                ];

                const result = transformTopPagesTableData(mockData as TopPerformaceTableEntity[]);
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
                        'request.createdAt': '2023-12-01T00:00:00Z',
                        'request.createdAt.day': '2023-12-01',
                        'request.totalRequest': '100'
                    },
                    {
                        'request.createdAt': '2023-12-02T00:00:00Z',
                        'request.createdAt.day': '2023-12-02',
                        'request.totalRequest': '150'
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
                        'request.createdAt': '2023-12-03T00:00:00Z',
                        'request.createdAt.day': '2023-12-03',
                        'request.totalRequest': '200'
                    },
                    {
                        'request.createdAt': '2023-12-01T00:00:00Z',
                        'request.createdAt.day': '2023-12-01',
                        'request.totalRequest': '100'
                    },
                    {
                        'request.createdAt': '2023-12-02T00:00:00Z',
                        'request.createdAt.day': '2023-12-02',
                        'request.totalRequest': '150'
                    }
                ];

                const result = transformPageViewTimeLineData(mockData);

                // Should be sorted chronologically
                expect(result.datasets[0].data).toEqual([100, 150, 200]);
            });

            it('should handle missing totalRequest fields', () => {
                const mockData: Partial<PageViewTimeLineEntity>[] = [
                    {
                        'request.createdAt': '2023-12-01T00:00:00Z',
                        'request.createdAt.day': '2023-12-01'
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
                            'request.createdAt': new Date(
                                baseDate.getTime() - 3 * 60 * 60 * 1000
                            ).toISOString(), // 9 AM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() + 2 * 60 * 60 * 1000
                            ).toISOString(), // 2 PM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '150'
                        },
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() + 6 * 60 * 60 * 1000
                            ).toISOString(), // 6 PM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '200'
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
                            'request.createdAt': '2023-12-01T12:00:00',
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2023-12-02T12:00:00',
                            'request.createdAt.day': '2023-12-02',
                            'request.totalRequest': '150'
                        },
                        {
                            'request.createdAt': '2023-12-03T12:00:00',
                            'request.createdAt.day': '2023-12-03',
                            'request.totalRequest': '200'
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
                            'request.createdAt': new Date(
                                baseDate.getTime() - 12 * 60 * 60 * 1000
                            ).toISOString(), // Midnight
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '50'
                        },
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() + 11 * 60 * 60 * 1000
                            ).toISOString(), // 11 PM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '75'
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
                            'request.createdAt': '2023-12-01T12:00:00.000', // Noon UTC - safe for most timezones
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2023-12-03T12:00:00.000', // Two days later at noon UTC
                            'request.createdAt.day': '2023-12-03',
                            'request.totalRequest': '120'
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
                    const baseDate = new Date('2023-12-01T12:00:00');
                    const unorderedSameDayData: PageViewTimeLineEntity[] = [
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() + 3 * 60 * 60 * 1000
                            ).toISOString(), // 3 PM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '200'
                        },
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() - 3 * 60 * 60 * 1000
                            ).toISOString(), // 9 AM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': new Date(
                                baseDate.getTime() + 9 * 60 * 60 * 1000
                            ).toISOString(), // 9 PM
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(unorderedSameDayData);

                    // Should be sorted chronologically: 9 AM, 3 PM, 9 PM
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
                            'request.createdAt': '2023-12-01T14:00:00.000', // 2 PM UTC (from endpoint format)
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2023-12-01T18:30:00.000', // 6:30 PM UTC (from endpoint format)
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '150'
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
                            'request.createdAt': '2023-12-01T22:00:00.000', // 10 PM UTC (endpoint format)
                            'request.createdAt.day': '2023-12-01',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2023-12-02T02:00:00.000', // 2 AM UTC next day (endpoint format)
                            'request.createdAt.day': '2023-12-02',
                            'request.totalRequest': '150'
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
                            'request.createdAt': '2025-08-05T16:00:00.000', // Endpoint format (no Z)
                            'request.createdAt.day': '2025-08-05',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2025-08-05T20:00:00.000', // Endpoint format (no Z)
                            'request.createdAt.day': '2025-08-05',
                            'request.totalRequest': '150'
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

                it('should handle mixed date formats (with and without Z)', () => {
                    // Test mixing endpoint format and standard UTC format
                    const mockData: PageViewTimeLineEntity[] = [
                        {
                            'request.createdAt': '2025-08-05T16:00:00.000', // Endpoint format (no Z)
                            'request.createdAt.day': '2025-08-05',
                            'request.totalRequest': '100'
                        },
                        {
                            'request.createdAt': '2025-08-05T20:00:00.000Z', // Standard UTC format (with Z)
                            'request.createdAt.day': '2025-08-05',
                            'request.totalRequest': '150'
                        }
                    ];

                    const result = transformPageViewTimeLineData(mockData);

                    // Should handle both formats correctly
                    expect(result.labels).toHaveLength(2);
                    expect(result.datasets[0].data).toEqual([100, 150]);

                    // Both should format as time (same day) - HH:mm format
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
                        'request.totalRequest': '500'
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1',
                        'request.totalRequest': '300'
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
                        'request.totalRequest': '0'
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
                        'request.totalRequest': '200'
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.totalRequest': '300'
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
                        'request.totalRequest': '100' // Less usage
                    },
                    {
                        'request.userAgent':
                            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                        'request.totalRequest': '500' // More usage
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
                        'request.totalRequest': '100'
                    },
                    {
                        'request.totalRequest': '200'
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
    });
});
