import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of, zip } from 'rxjs';

import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    BayesianStatusResponse,
    ComponentStatus,
    DEFAULT_VARIANT_ID,
    DotExperimentResults,
    DotExperimentStatus,
    DotExperimentVariantDetail,
    ReportSummaryLegendByBayesianStatus
} from '@dotcms/dotcms-models';
import {
    getExperimentMock,
    getExperimentResultsMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import {
    DotExperimentsReportsState,
    DotExperimentsReportsStore
} from './dot-experiments-reports-store';

const EXPERIMENT_MOCK = getExperimentMock(1);
const EXPERIMENT_MOCK_RESULTS = getExperimentResultsMock(0);

const ActivatedRouteMock = {
    snapshot: {
        params: {
            experimentId: EXPERIMENT_MOCK.id,
            pageId: EXPERIMENT_MOCK.pageId
        }
    }
};

const messageServiceMock = new MockDotMessageService({
    Sunday: 'Sunday',
    Monday: 'Monday',
    Tuesday: 'Tuesday',
    Wednesday: 'Wednesday',
    Thursday: 'Thursday',
    Friday: 'Friday',
    Saturday: 'Saturday',
    'months.january.short': 'Jan',
    'months.february.short': 'Feb',
    'months.march.short': 'Mar',
    'months.april.short': 'Apr',
    'months.may.short': 'May',
    'months.june.short': 'Jun',
    'months.july.short': 'Jul',
    'months.august.short': 'Aug',
    'months.september.short': 'Sep',
    'months.october.short': 'Oct',
    'months.november.short': 'Nov',
    'months.december.short': 'Dec'
});

describe('DotExperimentsReportsStore', () => {
    let spectator: SpectatorService<DotExperimentsReportsStore>;
    let store: DotExperimentsReportsStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createStoreService = createServiceFactory({
        service: DotExperimentsReportsStore,
        providers: [
            mockProvider(Title),
            {
                provide: ActivatedRoute,
                useValue: ActivatedRouteMock
            },
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(MessageService),
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createStoreService({});

        store = spectator.inject(DotExperimentsReportsStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.getResults.mockReturnValue(of(EXPERIMENT_MOCK_RESULTS));
    });

    it('should set initial data', (done) => {
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

        const expectedInitialState: DotExperimentsReportsState = {
            experiment: EXPERIMENT_MOCK,
            status: ComponentStatus.IDLE,
            results: EXPERIMENT_MOCK_RESULTS
        };

        expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);
        store.state$.subscribe((state) => {
            expect(state).toEqual(expectedInitialState);
            done();
        });
    });

    it('should have isLoading$ from the store', (done) => {
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);
        store.isLoading$.subscribe((data) => {
            expect(data).toEqual(false);
            done();
        });
    });

    it('should update component status to the store', (done) => {
        store.setComponentStatus(ComponentStatus.LOADED);
        store.state$.subscribe(({ status }) => {
            expect(status).toBe(ComponentStatus.LOADED);
            done();
        });
    });

    describe('Bayesian response map hasSession = 0', () => {
        it('should summaryWinnerLegend$ get `NO_WINNER_FOUND` when experiment status `ENDED` and any winnerSuggestion', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.ENDED
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 0 }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment.status).toEqual(DotExperimentStatus.ENDED);
            });
            store.summaryWinnerLegend$.subscribe((summaryWinnerLegend) => {
                expect(summaryWinnerLegend).toEqual(
                    ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
                );
                done();
            });
        });

        it('should summaryWinnerLegend$ get `NO_ENOUGH_SESSIONS` when experiment status `RUNNING` and any winnerSuggestion', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.RUNNING
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 0 }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.RUNNING);
                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS
                    );
                    done();
                }
            );
        });
    });
    describe('Bayesian response map hasSession > 0', () => {
        it('should summaryWinnerLegend$ get `NO_WINNER_FOUND` when experiment status `ENDED` and winnerSuggestion=`TIE`', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.ENDED
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: BayesianStatusResponse.TIE
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.ENDED);
                    expect(results.bayesianResult.suggestedWinner).toEqual(
                        BayesianStatusResponse.TIE
                    );

                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
                    );
                    done();
                }
            );
        });

        it('should summaryWinnerLegend$ get `NO_WINNER_FOUND` when experiment status `RUNNING` and winnerSuggestion=`TIE`', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.RUNNING
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: BayesianStatusResponse.TIE
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.RUNNING);
                    expect(results.bayesianResult.suggestedWinner).toEqual(
                        BayesianStatusResponse.TIE
                    );

                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
                    );
                    done();
                }
            );
        });

        it('should summaryWinnerLegend$ get `NO_WINNER_FOUND` when experiment status `ENDED` and winnerSuggestion=`NONE`', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.ENDED
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: BayesianStatusResponse.NONE
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.ENDED);
                    expect(results.bayesianResult.suggestedWinner).toEqual(
                        BayesianStatusResponse.NONE
                    );

                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.NO_WINNER_FOUND
                    );
                    done();
                }
            );
        });

        it('should summaryWinnerLegend$ get `NO_ENOUGH_SESSIONS` when experiment status `RUNNING` and winnerSuggestion=`NONE`', (done) => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.RUNNING
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: BayesianStatusResponse.NONE
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.RUNNING);
                    expect(results.bayesianResult.suggestedWinner).toEqual(
                        BayesianStatusResponse.NONE
                    );

                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.NO_ENOUGH_SESSIONS
                    );
                    done();
                }
            );
        });

        it('should summaryWinnerLegend$ get `WINNER` when experiment status `ENDED` and winnerSuggestion has a variantId`', (done) => {
            const winnerVariantId = EXPERIMENT_MOCK.trafficProportion.variants[0].id;

            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.ENDED
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: winnerVariantId
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.ENDED);
                    expect(results.bayesianResult.suggestedWinner).toEqual(winnerVariantId);

                    expect(summaryWinnerLegend).toEqual(ReportSummaryLegendByBayesianStatus.WINNER);
                    done();
                }
            );
        });

        it('should summaryWinnerLegend$ get `PRELIMINARY_WINNER` when experiment status `RUNNING` and winnerSuggestion has a variantId', (done) => {
            const winnerVariantId = EXPERIMENT_MOCK.trafficProportion.variants[0].id;

            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.RUNNING
                })
            );
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { ...EXPERIMENT_MOCK_RESULTS.sessions, total: 10 },
                    bayesianResult: {
                        ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                        suggestedWinner: winnerVariantId
                    }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            zip(store.state$, store.summaryWinnerLegend$).subscribe(
                ([{ experiment, results }, summaryWinnerLegend]) => {
                    expect(experiment.status).toEqual(DotExperimentStatus.RUNNING);
                    expect(results.bayesianResult.suggestedWinner).toEqual(winnerVariantId);

                    expect(summaryWinnerLegend).toEqual(
                        ReportSummaryLegendByBayesianStatus.PRELIMINARY_WINNER
                    );
                    done();
                }
            );
        });
    });

    describe('Effects', () => {
        it('should load experiment to store', (done) => {
            dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

            store.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            expect(dotExperimentsService.getById).toHaveBeenCalledWith(EXPERIMENT_MOCK.id);

            store.state$.subscribe(({ experiment }) => {
                expect(experiment).toEqual(EXPERIMENT_MOCK);
                done();
            });
        });

        it('should promote variant', () => {
            const variant: DotExperimentVariantDetail = {
                id: '111',
                name: 'Variant 111 Name',
                conversions: 0,
                conversionRate: '0%',
                conversionRateRange: '19.41% to 93.24%',
                sessions: 0,
                probabilityToBeBest: '7.69%',
                isWinner: false,
                isPromoted: false
            };

            dotExperimentsService.promoteVariant.mockReturnValue(of(EXPERIMENT_MOCK));

            store.promoteVariant({
                experimentId: EXPERIMENT_MOCK.id,
                variant: variant
            });

            expect(dotExperimentsService.promoteVariant).toHaveBeenCalledWith(
                EXPERIMENT_MOCK.id,
                variant.id
            );
        });
    });

    describe('chartJs parser', () => {
        beforeEach(() => {
            dotExperimentsService.getById.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK,
                    status: DotExperimentStatus.RUNNING
                })
            );
            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);
        });

        it('should get all the xLabels', (done) => {
            const expectedXLabels = [
                'Mar-31', // Day added manually when we parse the data
                'Apr-1',
                'Apr-2',
                'Apr-3',
                'Apr-4',
                'Apr-5',
                'Apr-6',
                'Apr-7',
                'Apr-8',
                'Apr-9',
                'Apr-10',
                'Apr-11',
                'Apr-12',
                'Apr-13',
                'Apr-14',
                'Apr-15'
            ];

            store.getDailyChartData$.subscribe(({ labels }) => {
                expect(labels.length).toEqual(expectedXLabels.length);
                expect(labels).toEqual(expectedXLabels);
                done();
            });
        });

        it('should has 2 datasets', (done) => {
            store.getDailyChartData$.subscribe(({ datasets }) => {
                expect(datasets.length).toEqual(
                    Object.keys(EXPERIMENT_MOCK_RESULTS.goals.primary.variants).length
                );
                done();
            });
        });

        it('should has a label and data properly parsed for each dataset', (done) => {
            // First data is added manually when we parse the data
            const expectedDataByDataset = [
                [0, 90.56, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15.25],
                [0, 15.25, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 90.56]
            ];
            const expectedLabel = [
                EXPERIMENT_MOCK_RESULTS.goals.primary.variants.DEFAULT.variantDescription,
                EXPERIMENT_MOCK_RESULTS.goals.primary.variants['111'].variantDescription
            ];

            store.getDailyChartData$.subscribe(({ datasets }) => {
                datasets.forEach((dataset, index) => {
                    const { label, data } = dataset;

                    expect(data).toEqual(expectedDataByDataset[index]);
                    expect(label).toEqual(expectedLabel[index]);
                });

                done();
            });
        });

        it('should generate the Pdfs data to render the Bayesian chart', (done) => {
            const EXPECTED_BAYESIAN_DATA_QTY = 100;
            const expectedLabel = [
                EXPERIMENT_MOCK_RESULTS.goals.primary.variants['111'].variantDescription,
                EXPERIMENT_MOCK_RESULTS.goals.primary.variants.DEFAULT.variantDescription
            ];

            store.getBayesianChartData$.subscribe(({ datasets }) => {
                datasets.forEach((dataset, index) => {
                    const { label, data } = dataset;

                    expect(data.length).toEqual(EXPECTED_BAYESIAN_DATA_QTY);
                    expect(label).toEqual(expectedLabel[index]);
                });

                done();
            });
        });
    });

    describe('Show Bayesian Chart', () => {
        function generateResutlsMock(
            winner: string,
            uniqueSessions: number[]
        ): DotExperimentResults {
            return {
                ...EXPERIMENT_MOCK_RESULTS,
                goals: {
                    ...EXPERIMENT_MOCK_RESULTS.goals,
                    primary: {
                        ...EXPERIMENT_MOCK_RESULTS.goals.primary,
                        variants: {
                            ...EXPERIMENT_MOCK_RESULTS.goals.primary.variants,
                            [DEFAULT_VARIANT_ID]: {
                                ...EXPERIMENT_MOCK_RESULTS.goals.primary.variants[
                                    DEFAULT_VARIANT_ID
                                ],
                                uniqueBySession: {
                                    count: uniqueSessions[0],
                                    totalPercentage: 100.0,
                                    variantPercentage: 100.0
                                }
                            },
                            '111': {
                                ...EXPERIMENT_MOCK_RESULTS.goals.primary.variants['111'],
                                uniqueBySession: {
                                    count: uniqueSessions[1],
                                    totalPercentage: 100.0,
                                    variantPercentage: 100.0
                                }
                            }
                        }
                    }
                },
                sessions: { total: 20, variants: { DEFAULT: 10, '111': 10 } },
                bayesianResult: {
                    ...EXPERIMENT_MOCK_RESULTS.bayesianResult,
                    suggestedWinner: winner
                }
            };
        }

        it('should `hasEnoughDataForShowBayesianChart$` retrieve `false` when the BayesianResult have `NONE` as suggested winner', (done) => {
            //The following mock has failures for both variants, enough sessions to display  results,
            // so it generate the data sets; but no suggested winner
            dotExperimentsService.getResults.mockReturnValue(
                of(generateResutlsMock(BayesianStatusResponse.NONE, [5, 8]))
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.vm$.subscribe((state) => {
                expect(state.bayesianChart.hasEnoughData).toEqual(false);
                done();
            });
        });

        it('should `hasEnoughDataForShowBayesianChart$` retrieve `false` when you have at least one dataset with empty data', (done) => {
            //The following mock don't have failures for the variant 111 ( second one ), have enough sessions to display  results,
            // have a suggested winner, but the variant 111 will not generate a date set
            dotExperimentsService.getResults.mockReturnValue(
                of(generateResutlsMock(DEFAULT_VARIANT_ID, [5, 10]))
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.vm$.subscribe((state) => {
                expect(state.bayesianChart.hasEnoughData).toEqual(false);
                done();
            });
        });

        it('should `hasEnoughDataForShowBayesianChart$` retrieve `true` when you have all dataset with a failure and a suggested winner', (done) => {
            //The following mock  have failures for both variants, have enough sessions to display  results,
            // have a suggested winner, and both variants will generate a date set
            dotExperimentsService.getResults.mockReturnValue(
                of(generateResutlsMock(DEFAULT_VARIANT_ID, [5, 5]))
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.vm$.subscribe((state) => {
                expect(state.bayesianChart.hasEnoughData).toEqual(true);
                done();
            });
        });
    });

    describe('Show Daily Chart', () => {
        it('should `dailyChart$` retrieve `false` when there is not enough sessions', (done) => {
            //The following mock have results but not enough sessions to display results
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.vm$.subscribe((state) => {
                expect(state.dailyChart.hasEnoughData).toEqual(false);
                done();
            });
        });

        it('should `dailyChart$` retrieve `true` when there is enough sessions', (done) => {
            //The following mock have results and more than 10 sessions.
            dotExperimentsService.getResults.mockReturnValue(
                of({
                    ...EXPERIMENT_MOCK_RESULTS,
                    sessions: { total: 20, variants: { DEFAULT: 10, '111': 10 } }
                })
            );

            spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

            store.vm$.subscribe((state) => {
                expect(state.dailyChart.hasEnoughData).toEqual(true);
                done();
            });
        });
    });

    it('should show the summary table data correctly', (done) => {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                status: DotExperimentStatus.RUNNING
            })
        );
        spectator.service.loadExperimentAndResults(EXPERIMENT_MOCK.id);

        const expectedResult = [
            {
                conversionRate: '0%',
                conversionRateRange: '19.4% to 93.2%',
                conversions: 0,
                id: '111',
                isPromoted: false,
                isWinner: false,
                name: 'Variant 111 Name',
                probabilityToBeBest: '7.69%',
                sessions: 0
            },
            {
                conversionRate: '100%',
                conversionRateRange: '66.4% to 99.7%',
                conversions: 2,
                id: 'DEFAULT',
                isPromoted: false,
                isWinner: false,
                name: 'DEFAULT Name',
                probabilityToBeBest: '92.3%',
                sessions: 2
            }
        ];

        store.vm$.subscribe(({ detailData }) => {
            expect(detailData).toEqual(expectedResult);
            done();
        });
    });
});
