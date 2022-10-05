import { DotExperimentsShellComponent } from './dot-experiments-shell.component';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { DotExperimentsStore } from '@portlets/dot-experiments/shared/stores/dot-experiments-store.service';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/experiments-header/dot-experiments-ui-header.component';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';

class ActivatedRouteMock {
    get parent() {
        return {
            parent: {
                snapshot: {
                    data: {
                        content: {
                            page: {
                                identifier: '1234',
                                title: 'My dotCMS experiment'
                            }
                        }
                    }
                }
            }
        };
    }
}

class RouterMock {
    navigate() {
        return true;
    }
}

describe('DotExperimentsShellComponent', () => {
    let spectator: Spectator<DotExperimentsShellComponent>;
    let dotExperimentsUiHeaderComponent: DotExperimentsUiHeaderComponent;

    const createComponent = createComponentFactory({
        imports: [
            HttpClientTestingModule,
            DotExperimentsUiHeaderComponent,
            DotLoadingIndicatorModule,
            RouterModule
        ],
        component: DotExperimentsShellComponent,
        providers: [
            DotExperimentsStore,
            {
                provide: ActivatedRoute,
                useClass: ActivatedRouteMock
            },
            {
                provide: Router,
                useClass: RouterMock
            },
            mockProvider(DotExperimentsService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has DotExperimentHeaderComponent', () => {
        const page = new ActivatedRouteMock().parent.parent.snapshot.data.content.page;
        dotExperimentsUiHeaderComponent = spectator.query(DotExperimentsUiHeaderComponent);

        expect(dotExperimentsUiHeaderComponent).toExist();
        expect(dotExperimentsUiHeaderComponent.title).toBe(page.title);
    });

    it('should call Navegate when click back', () => {
        const router = spectator.inject(Router);
        const navigateSpy = spyOn(router, 'navigate');

        spectator.component.goBack();

        expect(navigateSpy).toHaveBeenCalledWith(['edit-page/content'], {
            queryParamsHandling: 'preserve'
        });
    });
});
