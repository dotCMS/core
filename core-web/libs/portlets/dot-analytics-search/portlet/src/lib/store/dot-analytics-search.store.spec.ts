import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAnalyticsSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { AnalyticsQueryType, ComponentStatus } from '@dotcms/dotcms-models';

import { DotAnalyticsSearchStore } from './dot-analytics-search.store';

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
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotAnalyticsSearchService = spectator.inject(DotAnalyticsSearchService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should initialize with default state', () => {
        expect(store.isEnterprise()).toEqual(false);
        expect(store.results()).toEqual(null);
        expect(store.query()).toEqual({ value: null, type: AnalyticsQueryType.DEFAULT });
        expect(store.state()).toEqual(ComponentStatus.INIT);
        expect(store.errorMessage()).toEqual('');
    });

    describe('withMethods', () => {
        it('should set initial state', () => {
            store.initLoad(true);
            expect(store.isEnterprise()).toEqual(true);
        });

        it('should perform a POST request to the base URL and return results', () => {
            dotAnalyticsSearchService.get.mockReturnValue(of(mockResponse));

            store.getResults({ query: 'test' });

            expect(dotAnalyticsSearchService.get).toHaveBeenCalledWith(
                { query: 'test' },
                AnalyticsQueryType.DEFAULT
            );

            expect(store.results()).toEqual(mockResponse);
        });

        it('should handle error while getting results', () => {
            const mockError = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });

            dotAnalyticsSearchService.get.mockReturnValue(throwError(() => mockError));

            store.getResults({ query: 'test' });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
        });

        it('should update the query type and reset results', () => {
            dotAnalyticsSearchService.get.mockReturnValue(of(mockResponse));

            store.getResults({ query: 'test' });

            expect(store.results()).toEqual(mockResponse);

            store.updateQueryType(AnalyticsQueryType.CUBE);
            expect(store.query().type).toEqual(AnalyticsQueryType.CUBE);
            expect(store.results()).toBeNull();
        });
    });
});
