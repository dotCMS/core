import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { ComponentStore } from '@ngrx/component-store';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { MessageService } from 'primeng/api';
import { Toast, ToastModule } from 'primeng/toast';

import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotExperimentsStore } from '@portlets/dot-experiments/dot-experiments-shell/store/dot-experiments.store';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

import { DotExperimentsShellComponent } from './dot-experiments-shell.component';

const routerParamsPageId = '1111-1111-111';
const ActivatedRouteMock = {
    snapshot: {
        params: {
            pageId: routerParamsPageId
        },
        parent: { parent: { parent: { data: { content: { page: { title: '' } } } } } }
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
        imports: [
            HttpClientTestingModule,
            DotExperimentsUiHeaderComponent,
            DotLoadingIndicatorModule,
            RouterModule,
            ToastModule
        ],
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has Toast component', () => {
        expect(spectator.query(Toast)).toExist();
    });
});
