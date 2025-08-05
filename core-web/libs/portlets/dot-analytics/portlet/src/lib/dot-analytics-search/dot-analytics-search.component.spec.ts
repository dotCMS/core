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
                    'analytics.search.no.results': 'No results',
                    'analytics.search.execute.results': 'Execute a query to get results'
                })
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        data: {
                            isEnterprise: true
                        }
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotAnalyticsSearchStore, true);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
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

    it('should render the Splitter component', () => {
        spectator.detectChanges();
        expect(spectator.query(Splitter)).toExist();
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

    describe('Help Dialog', () => {
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

        it('should call setQuery when a query example button is clicked', fakeAsync(() => {
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
