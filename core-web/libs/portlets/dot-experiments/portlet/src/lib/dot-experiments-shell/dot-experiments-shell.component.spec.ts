import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { MessageService } from 'primeng/api';

import { DotExperimentsService } from '@dotcms/data-access';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { DotExperimentsStore } from './store/dot-experiments.store';

describe('DotExperimentsShellComponent', () => {
    let spectator: Spectator<DotExperimentsShellComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsShellComponent,
        imports: [DotExperimentsShellComponent],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            MessageService,
            DotExperimentsStore,
            DotExperimentsService
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('template', () => {
        it('should render router-outlet', () => {
            const routerOutlet = spectator.query('router-outlet');
            expect(routerOutlet).toBeTruthy();
        });

        it('should render p-toast', () => {
            const toast = spectator.query('p-toast');
            expect(toast).toBeTruthy();
        });
    });
});
