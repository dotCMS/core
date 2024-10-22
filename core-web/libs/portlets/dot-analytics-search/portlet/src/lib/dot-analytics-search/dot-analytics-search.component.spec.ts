import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsSearchComponent } from './dot-analytics-search.component';

import { DotAnalyticsSearchStore } from '../store/dot-analytics-search.store';

const messageServiceMock = new MockDotMessageService({
    'analytics.search.query': 'Query',
    'analytics.search.run.query': 'Run Query',
    'analytics.search.results': 'Results'
});

describe('DotAnalyticsSearchComponent', () => {
    let spectator: Spectator<DotAnalyticsSearchComponent>;
    let store: InstanceType<typeof DotAnalyticsSearchStore>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsSearchComponent,
        componentProviders: [DotAnalyticsSearchStore, DotAnalyticsSearchService],
        imports: [],
        mocks: [],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        data: {
                            isEnterprise: true
                        }
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },

            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAnalyticsSearchStore, true);
    });

    it('should initialize store with enterprise state on init', () => {
        const initLoadSpy = jest.spyOn(store, 'initLoad');
        spectator.detectChanges();
        expect(initLoadSpy).toHaveBeenCalledWith(true);
    });

    it('should call getResults with valid JSON', () => {
        const getResultsSpy = jest.spyOn(store, 'getResults');
        spectator.component.queryEditor = '{"measures": ["request.count"]}';
        spectator.detectChanges();
        const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
        spectator.click(button);

        expect(getResultsSpy).toHaveBeenCalledWith({ measures: ['request.count'] });
    });

    it('should not call getResults with invalid JSON', () => {
        const getResultsSpy = jest.spyOn(store, 'getResults');
        spectator.component.queryEditor = 'invalid json';
        spectator.detectChanges();
        const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
        spectator.click(button);

        expect(getResultsSpy).not.toHaveBeenCalled();
    });
});
