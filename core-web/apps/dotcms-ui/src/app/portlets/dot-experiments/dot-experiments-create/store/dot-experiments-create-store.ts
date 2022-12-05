import { Injectable } from '@angular/core';
import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { MessageService } from 'primeng/api';
import { DotMessageService } from '@dotcms/data-access';
import { Observable, throwError } from 'rxjs';
import { DotExperiment } from '@dotcms/dotcms-models';
import { switchMap, tap } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

export interface DotExperimentCreateStore {
    isOpenSidebar: boolean;
    isLoading: boolean;
}

const initialState: DotExperimentCreateStore = {
    isOpenSidebar: false,
    isLoading: false
};

@Injectable()
export class DotExperimentsCreateStore extends ComponentStore<DotExperimentCreateStore> {
    readonly isSaving$: Observable<boolean> = this.select(({ isLoading }) => isLoading);

    // Updaters
    readonly setIsSaving = this.updater((state) => ({
        ...state,
        isLoading: true
    }));
    readonly setOpenSlider = this.updater((state) => ({
        ...state,
        isOpenSidebar: true
    }));
    readonly setCloseSidebar = this.updater((state) => ({
        ...state,
        isOpenSidebar: false,
        isLoading: false
    }));

    // Effects
    readonly addExperiments = this.effect(
        (experiment$: Observable<Pick<DotExperiment, 'pageId' | 'name' | 'description'>>) => {
            return experiment$.pipe(
                tap(() => this.setIsSaving()),
                switchMap((experiment) =>
                    this.dotExperimentsService.add(experiment).pipe(
                        tapResponse(
                            (experiment) => {
                                this.messageService.add({
                                    severity: 'info',
                                    summary: this.dotMessageService.get(
                                        'experiments.action.add.confirm-title'
                                    ),
                                    detail: this.dotMessageService.get(
                                        'experiments.action.add.confirm-message',
                                        experiment.name
                                    )
                                });

                                this.router.navigate(
                                    [
                                        '/edit-page/experiments/configuration',
                                        experiment.pageId,
                                        experiment.id
                                    ],
                                    {
                                        queryParamsHandling: 'preserve'
                                    }
                                );
                            },
                            (error: HttpErrorResponse) => throwError(error),
                            () => this.setCloseSidebar()
                        )
                    )
                )
            );
        }
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly router: Router
    ) {
        super(initialState);
    }
}
