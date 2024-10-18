import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { byTestId } from '@ngneat/spectator/jest';

import { DotAnalyticsSearchComponent } from './dot-analytics-search.component';

import { DotAnalyticsSearchStore } from '../store/dot-analytics-search.store';

describe('DotAnalyticsSearchComponent', () => {
    let spectator: Spectator<DotAnalyticsSearchComponent>;
    let store: InstanceType<typeof DotAnalyticsSearchStore>;
    const createComponent = createComponentFactory({
        component: DotAnalyticsSearchComponent,
        mocks: [DotAnalyticsSearchStore]
    });

    beforeEach(() => {
        store = spectator.inject(DotAnalyticsSearchStore, true);
        spectator = createComponent();
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
