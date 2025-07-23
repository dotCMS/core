import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { Dialog, DialogModule } from 'primeng/dialog';
import { Splitter } from 'primeng/splitter';

import {
    DotAnalyticsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import DotAnalyticsSearchComponent from './dot-analytics-search.component';
import { DotAnalyticsSearchStore } from './store/dot-analytics-search.store';

describe('DotAnalyticsSearchComponent', () => {
    let spectator: Spectator<DotAnalyticsSearchComponent>;
    let store: InstanceType<typeof DotAnalyticsSearchStore>;

    const createComponent = createComponentFactory({
        component: DotAnalyticsSearchComponent,
        imports: [MockModule(MonacoEditorModule), ButtonModule, DialogModule],
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
                    'analytics.search.no.license': 'No license found',
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

            store.setQuery('{"measures": ["request.count"]}');

            spectator.detectChanges();

            const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
            spectator.click(button);

            expect(getResultsSpy).toHaveBeenCalled();
        });

        it('should not call getResults with invalid JSON', () => {
            store.setQuery('invalid json');

            spectator.detectChanges();

            const button = spectator.query(byTestId('run-query')) as HTMLButtonElement;
            spectator.click(button);

            expect(button).toBeDisabled();
        });

        it('should render the Splitter when healthCheck is "OK"', () => {
            spectator.detectChanges();
            expect(spectator.query(Splitter)).toExist();
        });

        describe('when the help dialog is displayed', () => {
            it('should display the help dialog when the help button is clicked', fakeAsync(() => {
                const helpButton = spectator.query(byTestId('help-button')) as HTMLButtonElement;
                spectator.click(helpButton);

                tick();
                spectator.detectChanges();

                const dialog = spectator.query(Dialog);

                expect(dialog).toExist();
                expect(dialog).toBeVisible();
            }));

            it('should display the correct number of query examples in the dialog', fakeAsync(() => {
                const helpButton = spectator.query(byTestId('help-button')) as HTMLButtonElement;
                spectator.click(helpButton);

                tick();
                spectator.detectChanges();

                const queryExamples = store.queryExamples();
                const exampleElements = spectator.queryAll(byTestId('query-example-container'));
                expect(exampleElements.length).toEqual(queryExamples.length);
            }));

            it('should call addExampleQuery when a query example button is clicked', fakeAsync(() => {
                const setQuerySpy = jest.spyOn(store, 'setQuery');
                const queryExamples = store.queryExamples();
                const helpButton = spectator.query(byTestId('help-button')) as HTMLButtonElement;
                spectator.click(helpButton);

                tick();
                spectator.detectChanges();

                spectator.click(byTestId('query-example-button'));

                expect(spectator.component.$showDialog()).toBeFalsy();
                expect(setQuerySpy).toHaveBeenCalledWith(queryExamples[0].query);
            }));
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
