import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { describe, expect, it, vi } from 'vitest';

import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotAnalyticsTrackerService } from '@dotcms/data-access';
import { EVENT_TYPES } from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { withTrack } from './withTrack';

import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';

const initialState = createInitialUVEState();

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
                    track: vi.fn()
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
                vi.useFakeTimers();
                vi.resetAllMocks();
            });

            afterEach(() => {
                vi.useRealTimers();
            });

            it('should call analyticsTracker.track with correct payload', () => {
                store.trackUVEModeChange({ fromMode: UVE_MODE.EDIT, toMode: UVE_MODE.PREVIEW });

                // This waits for the delay to pass
                vi.runAllTimers();

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
                vi.resetAllMocks();
                vi.useFakeTimers();
            });

            afterEach(() => {
                vi.useRealTimers();
            });

            it('should call analyticsTracker.track with correct payload', () => {
                const selectedDate = new Date().toISOString();

                store.trackUVECalendarChange({ selectedDate });

                // This waits for the delay to pass
                vi.runAllTimers();

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
