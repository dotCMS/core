import { DotExperiment } from '../../shared/models/dot-experiments.model';
import { of } from 'rxjs';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import {
    DotExperimentCreateStore,
    DotExperimentsCreateStore
} from '@portlets/dot-experiments/dot-experiments-create/store/dot-experiments-create-store.service';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotExperimentsListStore } from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { MessageService } from 'primeng/api';
import {
    DotExperimentsListStoreMock,
    DotExperimentsServiceMock
} from '@portlets/dot-experiments/test/mocks';

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
    const createStoreService = createServiceFactory({
        service: DotExperimentsCreateStore,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: DotExperimentsListStore,
                useValue: DotExperimentsListStoreMock
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
        const isSavingStateChangesExpected = [false, true, false];
        const isSavingSatuses = [];
        const experiment: Pick<DotExperiment, 'pageId' | 'name' | 'description'> = {
            pageId: '1111-1111-1111-1111',
            name: 'Experiment name',
            description: 'description or goal'
        };
        const spyDotExperimentService = spyOn(DotExperimentsServiceMock, 'add').and.callThrough();
        const spyMessageService = spyOn(MessageServiceMock, 'add').and.callThrough();
        const spyDotExperimentsListStore = spyOn(
            DotExperimentsListStoreMock,
            'addExperiment'
        ).and.callThrough();

        spectator.service.isSaving$.subscribe((isSaving) => {
            isSavingSatuses.push(isSaving);
        });

        spectator.service.addExperiments(experiment);

        expect(spyDotExperimentsListStore).toHaveBeenCalled();
        expect(spyDotExperimentService).toHaveBeenCalled();
        expect(spyMessageService).toHaveBeenCalled();
        expect(isSavingStateChangesExpected).toEqual(isSavingSatuses);
    });
});
