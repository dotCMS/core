import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';

import { DefaultGoalConfiguration, DotExperiment, Goals, Variant } from '@dotcms/dotcms-models';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';

import { DotExperimentsService } from './dot-experiments.service';

const API_ENDPOINT = '/api/v1/experiments';
const PAGE_Id = '123';
const EXPERIMENT_ID = ExperimentMocks[0].id;
const VARIANT_ID = ExperimentMocks[0].trafficProportion.variants[0].id;

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

    it('should get a list of experiments of pageId', () => {
        spectator.service.getAll(PAGE_Id).subscribe();
        spectator.expectOne(`${API_ENDPOINT}?pageId=${PAGE_Id}`, HttpMethod.GET);
    });

    it('should get an experiment by getById using experimentId', () => {
        spectator.service.getById(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.GET);
    });

    it('should archive a experiment with experimentId', () => {
        spectator.service.archive(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/_archive`, HttpMethod.PUT);
    });

    it('should delete a experiment with experimentId', () => {
        spectator.service.delete(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.DELETE);
    });

    it('should add a variant', () => {
        const variant: Pick<Variant, 'name'> = {
            name: 'cool name'
        };
        spectator.service.addVariant(EXPERIMENT_ID, variant).subscribe();
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

    it('should delete a variant with experimentId', () => {
        const variantIdToRemove = '11111111';
        spectator.service.removeVariant(EXPERIMENT_ID, variantIdToRemove).subscribe();
        spectator.expectOne(
            `${API_ENDPOINT}/${EXPERIMENT_ID}/variants/${variantIdToRemove}`,
            HttpMethod.DELETE
        );
    });

    it('should asign a goal to experiment ', () => {
        const goal: Goals = {
            ...DefaultGoalConfiguration
        };
        spectator.service.setGoal(EXPERIMENT_ID, goal).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.PATCH);
    });
});
