import { DotExperimentsService } from './dot-experiments.service';
import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';

const API_ENDPOINT = '/api/v1/experiments';
const PAGE_Id = '123';
const EXPERIMENT_ID = ExperimentMocks[0].id;

describe('DotExperimentsService', () => {
    let spectator: SpectatorHttp<DotExperimentsService>;
    const createHttp = createHttpFactory(DotExperimentsService);

    beforeEach(() => (spectator = createHttp()));

    it('should get an experiment by getById using experimentId', () => {
        spectator.service.getById(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.GET);
    });

    it('should get a list of experiments of pageId', () => {
        spectator.service.getAll(PAGE_Id).subscribe();
        spectator.expectOne(`${API_ENDPOINT}?pageId=${PAGE_Id}`, HttpMethod.GET);
    });

    it('should archive a experiment with experimentId', () => {
        spectator.service.archive(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}/_archive`, HttpMethod.PUT);
    });

    it('should delete a experiment with experimentId', () => {
        spectator.service.delete(EXPERIMENT_ID).subscribe();
        spectator.expectOne(`${API_ENDPOINT}/${EXPERIMENT_ID}`, HttpMethod.DELETE);
    });
});
