import { TestBed } from '@angular/core/testing';

import {
    DotExperimentsShellState,
    DotExperimentsShellStore
} from '@portlets/dot-experiments/dot-experiments-shell/store/dot-experiments-shell-store.service';
import { ActivatedRoute } from '@angular/router';
import { provideComponentStore } from '@ngrx/component-store';

class ActivatedRouteMock {
    get parent() {
        return {
            parent: {
                snapshot: {
                    data: {
                        content: {
                            page: {
                                identifier: '111-222',
                                title: 'My Page'
                            }
                        }
                    }
                }
            }
        };
    }
}

describe('DotExperimentsShellStore', () => {
    let store: DotExperimentsShellStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideComponentStore(DotExperimentsShellStore),
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                }
            ]
        });

        store = TestBed.inject(DotExperimentsShellStore);
    });

    it('should set initial data', (done) => {
        const expectedInitialState: DotExperimentsShellState = {
            pageTitle: 'My Page',
            pageId: '111-222'
        };

        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });
});
