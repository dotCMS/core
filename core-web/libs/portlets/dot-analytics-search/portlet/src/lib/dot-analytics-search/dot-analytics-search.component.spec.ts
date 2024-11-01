import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockModule } from 'ng-mocks';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { Splitter } from 'primeng/splitter';

import { DotAnalyticsSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';

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
        spectator.component.ngOnInit();

        expect(initLoadSpy).toHaveBeenCalledWith(true, HealthStatusTypes.OK);
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

    it('should  render dot-empty-container when healthCheck is not "OK"', () => {
        spectator.component.store.initLoad(true, HealthStatusTypes.NOT_CONFIGURED);

        spectator.detectChanges();

        const dotEmptyContainer = spectator.query(DotEmptyContainerComponent);
        expect(dotEmptyContainer).toExist();
    });
});
