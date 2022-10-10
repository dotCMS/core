import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { ComponentStore } from '@ngrx/component-store';
import { RenderedPageExperiments } from '@portlets/dot-experiments/shared/models/dot-experiments.model';

export interface DotExperimentsState {
    pageId: string;
    pageTitle: string;
}

@Component({
    selector: 'dot-experiments-shell',
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    readonly vm$: Observable<DotExperimentsState> = this.componentStore.state$;

    constructor(
        private readonly componentStore: ComponentStore<DotExperimentsState>,
        private readonly route: ActivatedRoute,
        private readonly router: Router
    ) {}

    ngOnInit() {
        const page = this._getResolverExperimentsData();
        this.componentStore.setState({
            pageId: page.identifier,
            pageTitle: page.title
        });
    }

    /**
     * Back to Edit Page / Content
     */
    goBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'preserve' });
    }

    private _getResolverExperimentsData(): RenderedPageExperiments {
        const { page } = this.route.parent?.parent.snapshot.data?.content as DotPageRenderState;

        return {
            identifier: page.identifier,
            title: page.title
        };
    }
}
