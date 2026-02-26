import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withFeature, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSPageAsset } from '@dotcms/types';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { withWorkflow } from './withWorkflow';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { Orientation, PageType, UVEState } from '../../models';
import { withPage } from '../page/withPage';

const pageParams: DotPageApiParams = {
    url: 'new-url',
    language_id: '1',
    [PERSONA_KEY]: '2'
};

const initialState: UVEState = {
    uveStatus: UVE_STATUS.LOADING,
    uveIsEnterprise: false,
    uveCurrentUser: null,
    flags: {},
    pageParams,
    pageLanguages: [],
    pageType: PageType.TRADITIONAL,
    pageExperiment: null,
    pageErrorCode: null,
    workflowActions: [],
    workflowIsLoading: false,
    workflowLockIsLoading: false,
    editorDragItem: null,
    editorBounds: [],
    editorState: EDITOR_STATE.IDLE,
    editorActiveContentlet: null,
    editorContentArea: null,
    editorPaletteOpen: true,
    editorRightSidebarOpen: false,
    editorOgTags: null,
    editorStyleSchemas: [],
    viewDevice: null,
    viewDeviceOrientation: Orientation.LANDSCAPE,
    viewSocialMedia: null,
    viewParams: null,
    viewOgTagsResults: null,
    viewZoomLevel: 1,
    viewZoomIsActive: false,
    viewZoomIframeDocHeight: 0,
    viewZoomGestureStartZoom: 1
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withFeature(() => withPage()),
    withWorkflow(),
    withMethods((store) => ({
        setPageAPIResponse: (pageAssetResponse: DotCMSPageAsset) => {
            store.setPageAssetResponse({ pageAsset: pageAssetResponse });
        }
    }))
);

describe('withLoad', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotWorkflowsActionsService: SpyObject<DotWorkflowsActionsService>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of(mockWorkflowsActions)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
    });

    it('should start with the initial state', () => {
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowIsLoading()).toBe(true);
    });

    it('should react to the pageAssetResponse', () => {
        store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowIsLoading()).toBe(true);
    });

    describe('withMethods', () => {
        describe('workflowFetch', () => {
            it('should call get workflow actions using the provided inode', () => {
                const spyWorkflowActions = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
                store.workflowFetch('123');
                expect(store.workflowIsLoading()).toBe(false);
                expect(store.workflowActions()).toEqual(mockWorkflowsActions);
                expect(spyWorkflowActions).toHaveBeenCalledWith('123');
            });
        });

        it('should set workflowIsLoading to true', () => {
            store.setWorkflowActionLoading(true);
            expect(store.workflowIsLoading()).toBe(true);
        });
    });

    afterEach(() => jest.clearAllMocks());
});
