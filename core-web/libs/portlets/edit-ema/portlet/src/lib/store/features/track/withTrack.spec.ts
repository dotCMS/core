import { describe, it, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotAnalyticsTrackerService } from '@dotcms/data-access';
import { EVENT_TYPES } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { withTrack } from './withTrack';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import { Orientation, UVEState } from '../../models';

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: {} as DotPageApiParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    isClientReady: false,
    // Phase 3: Nested editor state
    editor: {
        dragItem: null,
        bounds: [],
        state: EDITOR_STATE.IDLE,
        activeContentlet: null,
        contentArea: null,
        panels: {
            palette: { open: true },
            rightSidebar: { open: false }
        },
        ogTags: null,
        styleSchemas: []
    },
    // Phase 3: Nested toolbar state
    toolbar: {
        device: null,
        orientation: Orientation.LANDSCAPE,
        socialMedia: null,
        isEditState: true,
        isPreviewModeActive: false,
        ogTagsResults: null
    }
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withTrack());

describe('withTrack', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let analyticsTracker: DotAnalyticsTrackerService;
    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: jest.fn()
                }
            },
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        analyticsTracker = spectator.inject(DotAnalyticsTrackerService);
    });

    describe('methods', () => {
        describe('trackUVEModeChange', () => {
            beforeEach(() => {
                jest.useFakeTimers();
                jest.resetAllMocks();
            });

            afterEach(() => {
                jest.useRealTimers();
            });

            it('should call analyticsTracker.track with correct payload', () => {
                store.trackUVEModeChange({ fromMode: UVE_MODE.EDIT, toMode: UVE_MODE.PREVIEW });

                // This waits for the delay to pass
                jest.runAllTimers();

                expect(analyticsTracker.track).toHaveBeenCalledWith(EVENT_TYPES.UVE_MODE_CHANGE, {
                    fromMode: UVE_MODE.EDIT,
                    toMode: UVE_MODE.PREVIEW
                });
            });

            it('should not call analyticsTracker.track if the delay is not reached', () => {
                store.trackUVEModeChange({ fromMode: UVE_MODE.EDIT, toMode: UVE_MODE.PREVIEW });

                expect(analyticsTracker.track).not.toHaveBeenCalled();
            });
        });

        describe('trackUVECalendarChange', () => {
            beforeEach(() => {
                jest.resetAllMocks();
                jest.useFakeTimers();
            });

            afterEach(() => {
                jest.useRealTimers();
            });

            it('should call analyticsTracker.track with correct payload', () => {
                const selectedDate = new Date().toISOString();

                store.trackUVECalendarChange({ selectedDate });

                // This waits for the delay to pass
                jest.runAllTimers();

                expect(analyticsTracker.track).toHaveBeenCalledWith(
                    EVENT_TYPES.UVE_CALENDAR_CHANGE,
                    { selectedDate }
                );
            });

            it('should not call analyticsTracker.track if the delay is not reached', () => {
                store.trackUVECalendarChange({ selectedDate: new Date().toISOString() });

                expect(analyticsTracker.track).not.toHaveBeenCalled();
            });
        });
    });
});
