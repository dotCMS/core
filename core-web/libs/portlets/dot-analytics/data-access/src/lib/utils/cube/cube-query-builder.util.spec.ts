import { CubeQueryBuilder, createCubeQuery } from './cube-query-builder.util';

import { TimeRangeOptions } from '../../types';

describe('CubeQueryBuilder', () => {
    let builder: CubeQueryBuilder;

    beforeEach(() => {
        builder = new CubeQueryBuilder();
    });

    describe('Constructor and Basic Methods', () => {
        it('should create an empty query by default', () => {
            const query = builder.build();
            expect(query).toEqual({});
        });
    });

    describe('Measures', () => {
        it('should add measures with request prefix', () => {
            const query = builder.measures(['totalRequest', 'totalSessions']).build();
            expect(query.measures).toEqual(['request.totalRequest', 'request.totalSessions']);
        });

        it('should not duplicate request prefix if already present', () => {
            const query = builder.measures(['totalRequest']).build();
            expect(query.measures).toEqual(['request.totalRequest']);
        });
    });

    describe('Dimensions', () => {
        it('should add dimensions with request prefix', () => {
            const query = builder.dimensions(['path', 'pageTitle']).build();
            expect(query.dimensions).toEqual(['request.path', 'request.pageTitle']);
        });

        it('should not duplicate request prefix if already present', () => {
            const query = builder.dimensions(['path']).build();
            expect(query.dimensions).toEqual(['request.path']);
        });
    });

    describe('Pageviews Filter', () => {
        it('should add pageview filter', () => {
            const query = builder.pageviews().build();
            expect(query.filters).toEqual([
                {
                    member: 'request.eventType',
                    operator: 'equals',
                    values: ['pageview']
                }
            ]);
        });
    });

    describe('Site ID Filter', () => {
        it('should add site ID filter with single value', () => {
            const query = builder.siteId('site-123').build();
            expect(query.filters).toEqual([
                {
                    member: 'request.siteId',
                    operator: 'equals',
                    values: ['site-123']
                }
            ]);
        });

        it('should add site ID filter with multiple values', () => {
            const query = builder.siteId(['site-123', 'site-456']).build();
            expect(query.filters).toEqual([
                {
                    member: 'request.siteId',
                    operator: 'equals',
                    values: ['site-123', 'site-456']
                }
            ]);
        });

        it('should combine pageviews and siteId filters', () => {
            const query = builder.pageviews().siteId('site-123').build();
            expect(query.filters).toEqual([
                {
                    member: 'request.eventType',
                    operator: 'equals',
                    values: ['pageview']
                },
                {
                    member: 'request.siteId',
                    operator: 'equals',
                    values: ['site-123']
                }
            ]);
        });
    });

    describe('Custom Filters', () => {
        it('should add custom filter', () => {
            const query = builder.filter('path', 'equals', ['value1', 'value2']).build();
            expect(query.filters).toEqual([
                {
                    member: 'request.path',
                    operator: 'equals',
                    values: ['value1', 'value2']
                }
            ]);
        });

        it('should add multiple filters at once', () => {
            const filters = [
                { member: 'path', operator: 'equals' as const, values: ['val1'] },
                { member: 'userAgent', operator: 'contains' as const, values: ['val2'] }
            ];
            const query = builder.filters(filters).build();
            expect(query.filters).toEqual([
                {
                    member: 'request.path',
                    operator: 'equals',
                    values: ['val1']
                },
                {
                    member: 'request.userAgent',
                    operator: 'contains',
                    values: ['val2']
                }
            ]);
        });

        it('should combine different filter methods', () => {
            const query = builder
                .pageviews()
                .siteId('site-123')
                .filter('path', 'equals', ['value1'])
                .build();

            expect(query.filters).toEqual([
                {
                    member: 'request.eventType',
                    operator: 'equals',
                    values: ['pageview']
                },
                {
                    member: 'request.siteId',
                    operator: 'equals',
                    values: ['site-123']
                },
                {
                    member: 'request.path',
                    operator: 'equals',
                    values: ['value1']
                }
            ]);
        });
    });

    describe('Time Range', () => {
        it('should add time range with string date range', () => {
            const query = builder.timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS).build();
            expect(query.timeDimensions).toEqual([
                {
                    dimension: 'request.createdAt',
                    dateRange: TimeRangeOptions.LAST_7_DAYS
                }
            ]);
        });

        it('should add time range with granularity', () => {
            const query = builder
                .timeRange('createdAt', TimeRangeOptions.LAST_30_DAYS, 'week')
                .build();
            expect(query.timeDimensions).toEqual([
                {
                    dimension: 'request.createdAt',
                    dateRange: TimeRangeOptions.LAST_30_DAYS,
                    granularity: 'week'
                }
            ]);
        });
    });

    describe('Ordering', () => {
        it('should add order by field with default descending direction', () => {
            const query = builder.orderBy('totalRequest').build();
            expect(query.order).toEqual({ 'request.totalRequest': 'desc' });
        });

        it('should add order by field with ascending direction', () => {
            const query = builder.orderBy('createdAt', 'asc').build();
            expect(query.order).toEqual({ 'request.createdAt': 'asc' });
        });
    });

    describe('Limit', () => {
        it('should set query limit', () => {
            const query = builder.limit(50).build();
            expect(query.limit).toBe(50);
        });
    });

    describe('Complete Query Building for 6 Requests', () => {
        it('should build totalPageViews query', () => {
            const query = createCubeQuery()
                .measures(['totalRequest'])
                .pageviews()
                .siteId('site-123')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS)
                .build();

            expect(query).toEqual({
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS
                    }
                ]
            });
        });

        it('should build uniqueVisitors query', () => {
            const query = createCubeQuery()
                .measures(['totalSessions'])
                .pageviews()
                .siteId('site-123')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS)
                .build();

            expect(query).toEqual({
                measures: ['request.totalSessions'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS
                    }
                ]
            });
        });

        it('should build topPagePerformance query', () => {
            const query = createCubeQuery()
                .measures(['totalRequest'])
                .pageviews()
                .siteId('site-123')
                .orderBy('totalRequest', 'desc')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS)
                .limit(1)
                .build();

            expect(query).toEqual({
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                order: { 'request.totalRequest': 'desc' },
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS
                    }
                ],
                limit: 1
            });
        });

        it('should build pageViewTimeLine query', () => {
            const query = createCubeQuery()
                .measures(['totalRequest'])
                .pageviews()
                .siteId('site-123')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS, 'day')
                .build();

            expect(query).toEqual({
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS,
                        granularity: 'day'
                    }
                ]
            });
        });

        it('should build pageViewDeviceBrowsers query', () => {
            const query = createCubeQuery()
                .dimensions(['userAgent'])
                .measures(['totalRequest'])
                .pageviews()
                .siteId('site-123')
                .orderBy('totalRequest', 'desc')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS)
                .limit(10)
                .build();

            expect(query).toEqual({
                dimensions: ['request.userAgent'],
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                order: { 'request.totalRequest': 'desc' },
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS
                    }
                ],
                limit: 10
            });
        });

        it('should build getTopPagePerformanceTable query', () => {
            const query = createCubeQuery()
                .dimensions(['path', 'pageTitle'])
                .measures(['totalRequest'])
                .pageviews()
                .siteId('site-123')
                .orderBy('totalRequest', 'desc')
                .timeRange('createdAt', TimeRangeOptions.LAST_7_DAYS)
                .limit(50)
                .build();

            expect(query).toEqual({
                dimensions: ['request.path', 'request.pageTitle'],
                measures: ['request.totalRequest'],
                filters: [
                    {
                        member: 'request.eventType',
                        operator: 'equals',
                        values: ['pageview']
                    },
                    {
                        member: 'request.siteId',
                        operator: 'equals',
                        values: ['site-123']
                    }
                ],
                order: { 'request.totalRequest': 'desc' },
                timeDimensions: [
                    {
                        dimension: 'request.createdAt',
                        dateRange: TimeRangeOptions.LAST_7_DAYS
                    }
                ],
                limit: 50
            });
        });
    });
});

describe('createCubeQuery', () => {
    it('should create a new CubeQueryBuilder instance', () => {
        const builder = createCubeQuery();
        expect(builder).toBeInstanceOf(CubeQueryBuilder);
    });

    it('should create independent instances', () => {
        const builder1 = createCubeQuery().dimensions(['path']);
        const builder2 = createCubeQuery().dimensions(['userAgent']);

        const query1 = builder1.build();
        const query2 = builder2.build();

        expect(query1.dimensions).toEqual(['request.path']);
        expect(query2.dimensions).toEqual(['request.userAgent']);
    });
});
