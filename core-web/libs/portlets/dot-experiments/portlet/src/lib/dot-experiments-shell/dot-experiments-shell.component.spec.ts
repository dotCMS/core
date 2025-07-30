import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { ComponentStore, provideComponentStore } from '@ngrx/component-store';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { UVE_MODE } from '@dotcms/types';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotExperimentsStore } from './store/dot-experiments.store';
const routerParamsPageId = '1111-1111-111';
const ActivatedRouteMock = {
    snapshot: {
        params: {
            pageId: routerParamsPageId
        },
        parent: { parent: { data: { content: { page: { title: 'Experiment page title' } } } } },
        queryParams: {
            mode: UVE_MODE.EDIT,
            variantName: 'variantName',
            experimentId: 'experimentId'
        }
    }
};

class RouterMock {
    navigate() {
        return true;
    }
}

/**
 * Override the snapshot of the activated route
 * To simulate the queryParams change
 *
 * Note: Be sure run this before your first `spectator.detectChanges()`
 *
 * @param {*} activatedRoute
 * @param {*} mock
 */
const overrideRouteSnashot = (activatedRoute, mock) => {
    Object.defineProperty(activatedRoute, 'snapshot', {
        value: mock,
        writable: true // Allows mocking changes
    });
};

describe('DotExperimentsShellComponent', () => {
    let spectator: Spectator<DotExperimentsShellComponent>;
    let activatedRoute: ActivatedRoute;
    let router: Router;
    let routerSpy: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsShellComponent,
        providers: [
            ComponentStore,
            MessageService,
            DotExperimentsStore,
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            {
                provide: Router,
                useClass: RouterMock
            },
            mockProvider(provideComponentStore)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        activatedRoute = spectator.inject(ActivatedRoute, true);
        router = spectator.inject(Router, true);

        routerSpy = jest.spyOn(router, 'navigate');
    });

    it('should has Toast component', () => {
        expect(spectator.query(Toast)).toExist();
    });

    describe('remove queryParams', () => {
        it('should remove queryParams when variantName, mode and experimentId is present', () => {
            spectator.component.ngOnInit(); // At this point it was called but we didn't have the spy yet, so we need to call it again

            expect(routerSpy).toHaveBeenCalledWith([], {
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        it('should not remove queryParams when variantName, mode and experimentId is not present', () => {
            overrideRouteSnashot(activatedRoute, {
                params: {
                    pageId: routerParamsPageId
                },
                parent: {
                    parent: { data: { content: { page: { title: 'Experiment page title' } } } }
                },
                queryParams: {} // No queryParams should not trigger the navigation
            });

            spectator.detectChanges();

            expect(routerSpy).not.toHaveBeenCalled();
        });
    });
});
