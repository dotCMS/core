import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { AnalyticsQueryType, ComponentStatus, HealthStatusTypes } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsSearchStore } from './dot-analytics-search.store';

import { AnalyticsQueryExamples } from '../utils';

const mockResponse = [
    {
        'request.count': '5',
        'request.pageId': null,
        'request.pageTitle': null,
        'request.url':
            '/dA/6a8102b5-fdb0-4ad5-9a5d-e982bcdb54c8/image/320maxh/270ch/270cw/80q/13ro/$dotContentMap.image.name'
    },
    {
        'request.count': '5',
        'request.pageId': 'a9f30020-54ef-494e-92ed-645e757171c2',
        'request.pageTitle': 'Home',
        'request.url': '/'
    }
];

describe('DotAnalyticsSearchStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAnalyticsSearchStore>>;
    let store: InstanceType<typeof DotAnalyticsSearchStore>;
    let dotAnalyticsSearchService: SpyObject<DotAnalyticsSearchService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotAnalyticsSearchStore,
        providers: [
            mockProvider(DotAnalyticsSearchService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'analytics.search.no.configured': 'No configuration found',
                    'analytics.search.no.configured.subtitle':
                        'Please configure the analytics search',
                    'analytics.search.config.error': 'Configuration error',
                    'analytics.search.config.error.subtitle':
                        'There was an error in the configuration',
                    'analytics.search.no.license': 'No license found',
                    'analytics.search.no.license.subtitle': 'Please provide a valid license',
                    'analytics.search.no.results': 'No results',
                    'analytics.search.execute.results': 'Execute a query to get results'
                })
            }
        ]
    });

    describe('initial state', () => {
        beforeEach(() => {
            spectator = createService({
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: {
                                data: {
                                    isEnterprise: true,
                                    healthCheck: HealthStatusTypes.OK
                                }
                            }
                        }
                    }
                ]
            });
            store = spectator.service;
        });

        it('should initialize with default state', () => {
            expect(store.isEnterprise()).toEqual(true);
            expect(store.results()).toEqual('');
            expect(store.query()).toEqual({
                value: '',
                type: AnalyticsQueryType.CUBE,
                isValidJson: false
            });
            expect(store.state()).toEqual(ComponentStatus.INIT);
            expect(store.healthCheck()).toEqual(HealthStatusTypes.OK);
            expect(store.wallEmptyConfig()).toEqual(null);
            expect(store.emptyResultsConfig()).toEqual({
                icon: 'pi-search',
                subtitle: 'Execute a query to get results',
                title: 'No results'
            });
            expect(store.queryExamples()).toEqual(AnalyticsQueryExamples);
        });
    });

    describe('withMethods', () => {
        beforeEach(() => {
            spectator = createService({
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: {
                                data: {
                                    isEnterprise: true,
                                    healthCheck: HealthStatusTypes.OK
                                }
                            }
                        }
                    }
                ]
            });
            store = spectator.service;
            dotAnalyticsSearchService = spectator.inject(DotAnalyticsSearchService);
            dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        });

        it('should update the query state with a valid JSON query', () => {
            const validQuery = '{"measures": ["request.count"]}';
            store.setQuery(validQuery);

            expect(store.query().value).toBe(validQuery);
            expect(store.query().isValidJson).toBe(true);
        });

        it('should update the query state with an invalid JSON query', () => {
            const invalidQuery = 'invalid json';
            store.setQuery(invalidQuery);

            expect(store.query().value).toBe(invalidQuery);
            expect(store.query().isValidJson).toBe(false);
        });

        it('should perform a POST request to the base URL and return results', () => {
            store.setQuery('{"measures": ["request.count"]}');

            dotAnalyticsSearchService.get.mockReturnValue(of(mockResponse));

            store.getResults();

            expect(dotAnalyticsSearchService.get).toHaveBeenCalledWith(
                { measures: ['request.count'] },
                AnalyticsQueryType.CUBE
            );

            expect(store.results()).toEqual(JSON.stringify(mockResponse, null, 2));
        });

        it('should handle error while getting results', () => {
            const mockError = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });

            dotAnalyticsSearchService.get.mockReturnValue(throwError(() => mockError));

            store.getResults();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        });
    });

    describe('onInit', () => {
        describe('when isEnterprise is true', () => {
            describe('and healthCheck is "NOT_CONFIGURED"', () => {
                beforeEach(() => {
                    spectator = createService({
                        providers: [
                            {
                                provide: ActivatedRoute,
                                useValue: {
                                    snapshot: {
                                        data: {
                                            isEnterprise: true,
                                            healthCheck: HealthStatusTypes.NOT_CONFIGURED
                                        }
                                    }
                                }
                            }
                        ]
                    });
                    store = spectator.service;
                });

                it('should initialize with default state', () => {
                    expect(store.wallEmptyConfig()).toEqual({
                        icon: 'pi-search',
                        subtitle: 'Please configure the analytics search',
                        title: 'No configuration found'
                    });
                });
            });

            describe('and healthCheck is "CONFIGURATION_ERROR"', () => {
                beforeEach(() => {
                    spectator = createService({
                        providers: [
                            {
                                provide: ActivatedRoute,
                                useValue: {
                                    snapshot: {
                                        data: {
                                            isEnterprise: true,
                                            healthCheck: HealthStatusTypes.CONFIGURATION_ERROR
                                        }
                                    }
                                }
                            }
                        ]
                    });
                    store = spectator.service;
                });

                it('should set the wall empty configuration', () => {
                    expect(store.wallEmptyConfig()).toEqual({
                        icon: 'pi-search',
                        subtitle: 'There was an error in the configuration',
                        title: 'Configuration error'
                    });
                });
            });
        });

        describe('when isEnterprise is false', () => {
            beforeEach(() => {
                spectator = createService({
                    providers: [
                        {
                            provide: ActivatedRoute,
                            useValue: {
                                snapshot: {
                                    data: {
                                        isEnterprise: false,
                                        healthCheck: HealthStatusTypes.NOT_CONFIGURED
                                    }
                                }
                            }
                        }
                    ]
                });
                store = spectator.service;
            });

            it('should set the wall empty configuration', () => {
                expect(store.wallEmptyConfig()).toEqual({
                    icon: 'pi-search',
                    subtitle: 'Please provide a valid license',
                    title: 'No license found'
                });
            });
        });
    });
});
