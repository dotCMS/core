import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { Splitter } from 'primeng/splitter';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAnalyticsSearchComponent } from './dot-analytics-search.component';

import { DotAnalyticsSearchStore } from '../store/dot-analytics-search.store';

describe('DotAnalyticsSearchComponent', () => {
    let spectator: Spectator<DotAnalyticsSearchComponent>;
    let store: InstanceType<typeof DotAnalyticsSearchStore>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsSearchComponent,
        imports: [MockModule(MonacoEditorModule)],
        componentProviders: [DotAnalyticsSearchStore, DotAnalyticsSearchService],
        declarations: [],
        mocks: [],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),

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
                    'analytics.search.no.licence': 'No license found',
                    'analytics.search.no.license.subtitle': 'Please provide a valid license',
                    'analytics.search.no.results': 'No results',
                    'analytics.search.execute.results': 'Execute a query to get results'
                })
            }
        ]
    });

    describe('when healthCheck is "OK"', () => {
        beforeEach(() => {
            spectator = createComponent({
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
            store = spectator.inject(DotAnalyticsSearchStore, true);
        });

        it('should render dot-empty-container with the correct configuration', () => {
            spectator.detectChanges();
            const dotEmptyContainer = spectator.query(DotEmptyContainerComponent);
            expect(dotEmptyContainer).toExist();
            expect(dotEmptyContainer.configuration).toEqual({
                subtitle: 'Execute a query to get results',
                icon: 'pi-search',
                title: 'No results'
            });
        });

        it('should call getResults with valid JSON', () => {
            const getResultsSpy = jest.spyOn(store, 'getResults');

            spectator.component.queryEditor = '{"measures": ["request.count"]}';
            spectator.component.handleQueryChange('{"measures": ["request.count"]}');
            spectator.detectChanges();

            const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
            spectator.click(button);

            expect(getResultsSpy).toHaveBeenCalledWith({ measures: ['request.count'] });
        });

        it('should not call getResults with invalid JSON', () => {
            spectator.component.queryEditor = 'invalid json';
            spectator.component.handleQueryChange('invalid json');
            spectator.detectChanges();

            const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
            spectator.click(button);

            expect(button).toBeDisabled();
        });

        it('should render the Splitter when healthCheck is "OK"', () => {
            spectator.detectChanges();
            expect(spectator.query(Splitter)).toExist();
        });
    });

    describe('when healthCheck is "NOT_CONFIGURED"', () => {
        beforeEach(() => {
            spectator = createComponent({
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
            store = spectator.inject(DotAnalyticsSearchStore, true);
        });

        it('should  render dot-empty-container', () => {
            spectator.detectChanges();
            const dotEmptyContainer = spectator.query(DotEmptyContainerComponent);
            expect(dotEmptyContainer).toExist();
        });
    });
});
