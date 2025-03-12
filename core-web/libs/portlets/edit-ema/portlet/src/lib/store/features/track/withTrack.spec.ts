import { describe } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotAnalyticsTrackerService } from '@dotcms/data-access';
import { EVENT_TYPES } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/uve/types';

import { TRACKING_DELAY } from './utils';
import { withTrack } from './withTrack';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { UVEState } from '../../models';

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
    canEditPage: false,
    pageIsLocked: true,
    isClientReady: false
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withTrack());

describe('withTrack', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let analyticsTracker: DotAnalyticsTrackerService;
    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [DotAnalyticsTrackerService]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        analyticsTracker = spectator.inject(DotAnalyticsTrackerService);
    });

    describe('methods', () => {
        describe('trackUVEModeChange', () => {
            it('should call analyticsTracker.track with correct payload', fakeAsync(() => {
                store.trackUVEModeChange({ fromMode: UVE_MODE.EDIT, toMode: UVE_MODE.PREVIEW });

                tick(TRACKING_DELAY + 1);

                expect(analyticsTracker.track).toHaveBeenCalledWith(EVENT_TYPES.UVE_MODE_CHANGE, {
                    fromMode: UVE_MODE.EDIT,
                    toMode: UVE_MODE.PREVIEW
                });
            }));

            it('should not call analyticsTracker.track if the delay is not reached', fakeAsync(() => {
                store.trackUVEModeChange({ fromMode: UVE_MODE.EDIT, toMode: UVE_MODE.PREVIEW });

                tick(TRACKING_DELAY - 10);

                expect(analyticsTracker.track).not.toHaveBeenCalled();
            }));
        });

        describe('trackUVECalendarChange', () => {
            it('should call analyticsTracker.track with correct payload', fakeAsync(() => {
                const selectedDate = new Date().toISOString();

                store.trackUVECalendarChange({ selectedDate });

                tick(TRACKING_DELAY + 1);

                expect(analyticsTracker.track).toHaveBeenCalledWith(
                    EVENT_TYPES.UVE_CALENDAR_CHANGE,
                    { selectedDate }
                );
            }));

            it('should not call analyticsTracker.track if the delay is not reached', fakeAsync(() => {
                store.trackUVECalendarChange({ selectedDate: new Date().toISOString() });

                tick(TRACKING_DELAY - 10);

                expect(analyticsTracker.track).not.toHaveBeenCalled();
            }));
        });
    });
});
