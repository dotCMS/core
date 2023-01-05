import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator';
import {
    DotExperimentCreateStore,
    DotExperimentsCreateStore
} from '@portlets/dot-experiments/dot-experiments-create/store/dot-experiments-create-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { DotExperimentsServiceMock } from '@portlets/dot-experiments/test/mocks';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';

const MessageServiceMock = {
    add: () => of({})
};

const initialState: DotExperimentCreateStore = {
    isOpenSidebar: false,
    isLoading: false
};

const messageServiceMock = new MockDotMessageService({
    'experimentspage.add.new.experiment': 'Add a new experiment'
});

describe('DotExperimentsCreateStore', () => {
    let spectator: SpectatorService<DotExperimentsCreateStore>;
    let dotExperimentsService: DotExperimentsService;
    const createStoreService = createServiceFactory({
        service: DotExperimentsCreateStore,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },

            {
                provide: DotExperimentsService,
                useValue: DotExperimentsServiceMock
            },
            {
                provide: MessageService,
                useValue: MessageServiceMock
            }
        ]
    });
    beforeEach(() => {
        spectator = createStoreService();
        dotExperimentsService = spectator.inject(DotExperimentsService);
    });
    it('should set initial data', () => {
        spectator.service.state$.subscribe((state) => {
            expect(state).toEqual(initialState);
        });
    });

    it('should update isSaving to the store', () => {
        spectator.service.setIsSaving();
        spectator.service.state$.subscribe(({ isLoading }) => {
            expect(isLoading).toBe(true);
        });
    });

    it('should update isOpenSlider and isSaving to the store', () => {
        spectator.service.setCloseSidebar();
        spectator.service.state$.subscribe(({ isOpenSidebar, isLoading }) => {
            expect(isOpenSidebar).toBe(false);
            expect(isLoading).toBe(false);
        });
    });

    it('should save the experiment', () => {
        const isSavingStateChangesExpected = [false, true];
        const isSavingSatuses = [];
        const experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'> = {
            pageId: '1111-1111-1111-1111',
            name: 'Experiment name',
            description: 'description or goal'
        };
        const spydotExperimentsService = spyOn(dotExperimentsService, 'add').and.callThrough();
        const spyMessageService = spyOn(MessageServiceMock, 'add').and.callThrough();

        spectator.service.isSaving$.subscribe((isSaving) => {
            isSavingSatuses.push(isSaving);
        });

        spectator.service.addExperiments(experiment);

        expect(spydotExperimentsService).toHaveBeenCalled();
        expect(spyMessageService).toHaveBeenCalled();
        expect(isSavingStateChangesExpected).toEqual(isSavingSatuses);
    });
});
