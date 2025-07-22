import {
    extractPageTitle,
    extractPageViews,
    extractSessions,
    extractTopPageValue,
    TablePageData,
    transformDeviceBrowsersData,
    transformPageViewTimeLineData,
    transformTopPagesTableData
} from './analytics-data.utils';

import type {
    PageViewDeviceBrowsersEntity,
    PageViewTimeLineEntity,
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

            it('should return NaN when totalRequest is missing', () => {
                const mockData: Partial<TotalPageViewsEntity> = {};

                const result = extractPageViews(mockData as TotalPageViewsEntity);
                expect(result).toBeNaN();
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
                    'request.totalUser': '342'
                };

                const result = extractSessions(mockData);
                expect(result).toBe(342);
            });

            it('should return 0 when data is null', () => {
                const result = extractSessions(null);
                expect(result).toBe(0);
            });

            it('should return NaN when totalUser is missing', () => {
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
});
