import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { DotExperimentsStore } from '../shared/stores/dot-experiments-store.service';
import { ActivatedRoute, Router } from '@angular/router';
import { DotExperimentsService } from '../shared/services/dot-experiments.service';
import { Observable } from 'rxjs';
import { LoadingState } from '@portlets/shared/models/shared-models';
import { RenderedPageExperiments } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';

interface VmShellExperiments {
    status: LoadingState;
    currentTitlePage: string;
}

@Component({
    selector: 'dot-experiments-shell',
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    providers: [DotExperimentsStore, DotExperimentsService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent implements OnInit {
    vmShell$: Observable<VmShellExperiments> = this.experimentsStore.vmShell$;

    constructor(
        private readonly experimentsStore: DotExperimentsStore,
        private readonly route: ActivatedRoute,
        private readonly router: Router
    ) {}

    ngOnInit() {
        this.experimentsStore.initStore(this._getResolverExperimentsData(this.route));
    }

    goBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'preserve' });
    }

    private _getResolverExperimentsData(route: ActivatedRoute): RenderedPageExperiments {
        const { page } = route.parent?.parent.snapshot.data?.content as DotPageRenderState;

        return {
            identifier: page.identifier,
            title: page.title
        };
    }
}
