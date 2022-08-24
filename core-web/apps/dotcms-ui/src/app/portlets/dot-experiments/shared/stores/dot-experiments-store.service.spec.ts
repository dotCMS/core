import { TestBed } from '@angular/core/testing';

import { DotExperimentsStore } from './dot-experiments-store.service';

import { LoadingState } from '@portlets/shared/models/shared-models';

describe('DotExperimentsStore', () => {
    let experimentsStore: DotExperimentsStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotExperimentsStore]
        });

        experimentsStore = TestBed.inject(DotExperimentsStore);
    });

    it('should set initial data', (done) => {
        const expectedInitialState = {
            pageId: '',
            state: LoadingState.INIT
        };

        experimentsStore.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    // Selectors
    it('should have getState$ Selector', () => {
        experimentsStore.getState$.subscribe((data) => {
            expect(data).toEqual(LoadingState.INIT);
        });
    });
});
