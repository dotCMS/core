import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { ComponentStore, provideComponentStore } from '@ngrx/component-store';

import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotExperimentsStore } from './store/dot-experiments.store';

const routerParamsPageId = '1111-1111-111';
const ActivatedRouteMock = {
    snapshot: {
        params: {
            pageId: routerParamsPageId
        },
        parent: { parent: { data: { content: { page: { title: 'Experiment page title' } } } } }
    }
};

class RouterMock {
    navigate() {
        return true;
    }
}

describe('DotExperimentsShellComponent', () => {
    let spectator: Spectator<DotExperimentsShellComponent>;

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
    });

    it('should has Toast component', () => {
        expect(spectator.query(Toast)).toExist();
    });
});
