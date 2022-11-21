import { Injectable } from '@angular/core';
import { ComponentStore, OnStateInit, tapResponse } from '@ngrx/component-store';
import { LoadingState } from '@portlets/shared/models/shared-models';
import { ActivatedRoute } from '@angular/router';
import {
    DotExperiment,
    TrafficProportion,
    Variant
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { Observable, pipe, throwError } from 'rxjs';
import { switchMap, tap, withLatestFrom } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { Title } from '@angular/platform-browser';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MessageService } from 'primeng/api';

export interface DotExperimentsConfigurationState {
    pageId: string;
    experimentId: string;
    experiment: DotExperiment | null;
    status: LoadingState;
    isSidebarOpen: boolean;
    isSaving: boolean;
}

const initialState: DotExperimentsConfigurationState = {
    pageId: '',
    experimentId: '',
    experiment: null,
    status: LoadingState.LOADING,
    isSidebarOpen: false,
    isSaving: false
};

@Injectable()
export class DotExperimentsConfigurationStore
    extends ComponentStore<DotExperimentsConfigurationState>
    implements OnStateInit
{
    // Selectors
    readonly isLoading$ = this.select(({ status }) => status === LoadingState.LOADING);

    readonly getExperimentId$ = this.select(({ experimentId }) => experimentId);

    readonly getTrafficProportion$ = this.select(({ experiment }) => experiment.trafficProportion);
    readonly isVariantStepDone$ = this.select(
        ({ experiment }) => experiment.trafficProportion.variants.length > 1
    );

    // Updaters
    readonly setComponentStatus = this.updater((state, status: LoadingState) => ({
        ...state,
        status
    }));
    readonly setIsSavingStatus = this.updater((state, isSaving: boolean) => ({
        ...state,
        isSaving
    }));
    readonly closeSidebar = this.updater((state) => ({
        ...state,
        isSidebarOpen: false,
        isSaving: false
    }));
    readonly openSidebar = this.updater((state) => ({
        ...state,
        isSidebarOpen: true
    }));

    readonly setExperiment = this.updater((state, experiment: DotExperiment) => ({
        ...state,
        status: LoadingState.LOADED,
        experiment: experiment
    }));
    readonly setTrafficProportion = this.updater((state, trafficProportion: TrafficProportion) => ({
        ...state,
        status: LoadingState.LOADED,
        experiment: { ...state.experiment, trafficProportion }
    }));

    // Effects
    readonly loadExperiment = this.effect<void>(
        pipe(
            tap(() => this.setComponentStatus(LoadingState.LOADING)),
            withLatestFrom(this.state$),
            switchMap(([, { experimentId }]) =>
                this.dotExperimentsService.getById(experimentId).pipe(
                    tapResponse(
                        (experiments) => {
                            this.setExperiment(experiments);
                            this.updateTitles(experiments);
                        },
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.setComponentStatus(LoadingState.LOADED)
                    )
                )
            )
        )
    );

    readonly addVariant = this.effect((variant$: Observable<Pick<DotExperiment, 'name'>>) => {
        return variant$.pipe(
            tap(() => this.setIsSavingStatus(true)),
            withLatestFrom(this.getExperimentId$),
            switchMap(([variant, experimentId]) =>
                this.dotExperimentsService.addVariant(experimentId, variant).pipe(
                    tapResponse(
                        (experiment) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.configure.variant.add.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.configure.variant.add.confirm-message',
                                    experiment.name
                                )
                            });

                            this.setTrafficProportion(experiment.trafficProportion);
                        },
                        (error: HttpErrorResponse) => throwError(error),
                        () => this.closeSidebar()
                    )
                )
            )
        );
    });

    readonly deleteVariant = this.effect((variant$: Observable<Variant>) => {
        return variant$.pipe(
            withLatestFrom(this.getExperimentId$),
            switchMap(([variant, experimentId]) =>
                this.dotExperimentsService.removeVariant(experimentId, variant.id).pipe(
                    tapResponse(
                        (experiment) => {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'experiments.configure.variant.delete.confirm-title'
                                ),
                                detail: this.dotMessageService.get(
                                    'experiments.configure.variant.delete.confirm-message',
                                    variant.name
                                )
                            });

                            this.setTrafficProportion(experiment.trafficProportion);
                        },
                        (error: HttpErrorResponse) => throwError(error)
                    )
                )
            )
        );
    });

    readonly vm$: Observable<{
        pageId: string;
        experimentId: string;
        experiment: DotExperiment;
        isLoading: boolean;
    }> = this.select(
        this.state$,
        this.isLoading$,
        ({ pageId, experiment, experimentId }, isLoading) => ({
            pageId,
            experiment,
            experimentId,
            isLoading
        })
    );
    readonly vmVariants$: Observable<{
        isSidebarOpen: boolean;
        isSaving: boolean;
        trafficProportion: TrafficProportion;
        isVariantStepDone: boolean;
    }> = this.select(
        this.state$,
        this.getTrafficProportion$,
        this.isVariantStepDone$,
        ({ isSidebarOpen, isSaving }, trafficProportion, isVariantStepDone) => ({
            isSidebarOpen,
            isSaving,
            trafficProportion,
            isVariantStepDone
        })
    );

    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotMessageService: DotMessageService,
        private readonly messageService: MessageService,
        private readonly route: ActivatedRoute,
        private readonly title: Title
    ) {
        super({
            ...initialState,
            experimentId: route.snapshot.params.experimentId,
            pageId: route.snapshot.params.pageId
        });
    }

    ngrxOnStateInit() {
        this.loadExperiment();
    }

    private updateTitles(experiment: DotExperiment) {
        this.title.setTitle(`${experiment.name} - ${this.title.getTitle()}`);
    }
}
