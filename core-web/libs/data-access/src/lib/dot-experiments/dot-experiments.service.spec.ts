import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/';

import {
    DefaultGoalConfiguration,
    DotExperiment,
    DotExperimentStatus,
    Goals,
    GoalsLevels,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { getExperimentMock } from '@dotcms/utils-testing';

import { DotExperimentsService } from './dot-experiments.service';

const API_ENDPOINT = '/api/v1/experiments';
const PAGE_Id = '123';
const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_ID = EXPERIMENT_MOCK.id;
const VARIANT_ID = EXPERIMENT_MOCK.trafficProportion.variants[0].id;

describe('DotExperimentsService', () => {
    let spectator: SpectatorHttp<DotExperimentsService>;
    const createHttp = createHttpFactory(DotExperimentsService);

    beforeEach(() => (spectator = createHttp()));

    it('should add an experiment', () => {
        const experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'> = {
            pageId: '11111',
            name: 'cool name',
            description: 'amazing description'
        };
        spectator.service.add(experiment).subscribe();
        spectator.expectOne(`${API_ENDPOINT}`, HttpMethod.POST);
    });

    it('should do the configuration healthcheck', () => {
        spectator.service.healthCheck().subscribe();
        spectator.expectOne('/api/v1/experiments/health', HttpMethod.GET);
    });

    it('should get a list of experiments of pageId', () => {
        spectator.service.getAll(PAGE_Id).subscribe();
        spectator.expectOne(`${API_ENDPOINT}?pageId=${PAGE_Id}`, HttpMethod.GET);
    });

    it('should get a list of experiments filter by status', () => {
        spectator.service.getByStatus(PAGE_Id, DotExperimentStatus.RUNNING).subscribe();
        spectator.expectOne(`${API_ENDPOINT}?pageId=${PAGE_Id}&status=RUNNING`, HttpMethod.GET);
    });

    it('should get an experiment by getById using experimentId', () => {
        spectator.service.getById(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.GET);
    });

    it('should get experiment results', () => {
        spectator.service.getResults(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/results`, HttpMethod.GET);
    });

    it('should archive an experiment with experimentId', () => {
        spectator.service.archive(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/_archive`, HttpMethod.PUT);
    });

    it('should start an experiment with experimentId as param', () => {
        spectator.service.start(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/_start`, HttpMethod.POST);
    });

    it('should stop an experiment with experimentId as param', () => {
        spectator.service.stop(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/_end`, HttpMethod.POST);
    });

    it('should cancel schedule an experiment with experimentId as param', () => {
        spectator.service.cancelSchedule(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/scheduled/${EXPERIMENT_ID}/_cancel`, HttpMethod.POST);
    });

    it('should delete a experiment with experimentId', () => {
        spectator.service.delete(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.DELETE);
    });

    it('should add a variant', () => {
        spectator.service.addVariant(EXPERIMENT_ID, 'cool name').subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/variants`, HttpMethod.POST);
    });

    it('should edit a variant experimentId', () => {
        spectator.service
            .editVariant(EXPERIMENT_ID, VARIANT_ID, { description: 'new-name' })
            .subscribe();
        spectator.expectOne(
            `${API_ENDPOINT}/${EXPERIMENT_ID}/variants/${VARIANT_ID}`,
            HttpMethod.PUT
        );
    });

    it('should promote a variant', () => {
        spectator.service.promoteVariant(EXPERIMENT_ID, VARIANT_ID).subscribe();
        spectator.expectOne(
            `/api/v1/experiments/${EXPERIMENT_ID}/variants/${VARIANT_ID}/_promote`,
            HttpMethod.PUT
        );
    });

    it('should delete a variant with experimentId', () => {
        const variantIdToRemove = '11111111';
        spectator.service.removeVariant(EXPERIMENT_ID, variantIdToRemove).subscribe();
        spectator.expectOne(
            `${API_ENDPOINT}/${EXPERIMENT_ID}/variants/${variantIdToRemove}`,
            HttpMethod.DELETE
        );
    });

    it('should assign a goal to experiment ', () => {
        const goal: Goals = {
            ...DefaultGoalConfiguration
        };
        spectator.service.setGoal(EXPERIMENT_ID, goal).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);
    });

    it('should delete a goal with experimentId', () => {
        const goalType: GoalsLevels = 'primary';
        spectator.service.deleteGoal(EXPERIMENT_ID, goalType).subscribe();
        spectator.expectOne(
            `${API_ENDPOINT}/${EXPERIMENT_ID}/goals/${goalType}`,
            HttpMethod.DELETE
        );
    });

    it('should change the description of an experimentId', () => {
        const newDescription = 'new description';
        spectator.service.setDescription(EXPERIMENT_ID, newDescription).subscribe();
        const req = spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);

        expect(req.request.body['description']).toEqual(newDescription);
    });

    it('should set scheduling to experiment', () => {
        const newScheduling = { startDate: 1, endDate: 2 };
        spectator.service.setScheduling(EXPERIMENT_ID, newScheduling).subscribe();

        const req = spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);

        expect(req.request.body['scheduling']).toEqual(newScheduling);
    });

    it('should set traffic allocation to experiment', () => {
        const newValue = 50;
        spectator.service.setTrafficAllocation(EXPERIMENT_ID, newValue).subscribe();

        const req = spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);

        expect(req.request.body['trafficAllocation']).toEqual(newValue);
    });

    it('should set traffic proportion to experiment', () => {
        const newValue = {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [{ id: '111', name: 'DEFAULT', weight: 100 }]
        };
        spectator.service.setTrafficProportion(EXPERIMENT_ID, newValue).subscribe();

        const req = spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);

        expect(req.request.body['trafficProportion']).toEqual(newValue);
    });

    it('should return an Observable of undefined when experimentId is undefined', (done) => {
        spectator.service.getById(undefined).subscribe((result) => {
            expect(result).toBeUndefined();
            done();
        });
    });
});
