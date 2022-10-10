import { DotExperimentsService } from './dot-experiments.service';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { of } from 'rxjs';
import {
    DotExperimentStatusList,
    TrafficProportionTypes
} from '@portlets/dot-experiments/shared/models/dot-experiments-constants';

const experimentsMock: DotExperiment[] = [
    {
        id: '111',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    },
    {
        id: '222',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];
describe('DotExperimentsService', () => {
    let dotExperimentsService: DotExperimentsService;
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotExperimentsService],
            imports: [HttpClientTestingModule]
        });
        dotExperimentsService = TestBed.inject(DotExperimentsService);
    });

    it('should get a list of experiments of pageId', () => {
        const pageId = '123';

        spyOn(dotExperimentsService, 'get').and.returnValue(of(experimentsMock));

        dotExperimentsService.get(pageId).subscribe((ex) => {
            expect(dotExperimentsService.get).toHaveBeenCalledWith(pageId);
            expect(ex).toEqual(experimentsMock);
        });
    });

    it('should archive a experiment with experimentId', () => {
        const esperimentId = experimentsMock[0].id;

        const dotExperimentsServiceSpy = spyOn(dotExperimentsService, 'archive').and.returnValue(
            of([experimentsMock[0]])
        );

        dotExperimentsService.archive(esperimentId).subscribe((ex) => {
            expect(dotExperimentsServiceSpy).toHaveBeenCalledWith(esperimentId);
            expect(ex).toEqual([experimentsMock[0]]);
        });
    });

    it('should delete a experiment with experimentId', () => {
        const esperimentId = experimentsMock[0].id;

        const dotExperimentsServiceSpy = spyOn(dotExperimentsService, 'delete').and.returnValue(
            of('Experiment deleted')
        );

        dotExperimentsService.delete(esperimentId).subscribe(() => {
            expect(dotExperimentsServiceSpy).toHaveBeenCalledWith(esperimentId);
        });
    });
});
