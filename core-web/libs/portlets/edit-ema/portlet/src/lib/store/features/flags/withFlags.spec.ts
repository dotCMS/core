import { describe } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { withFlags } from './withFlags';

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

const MOCK_UVE_FEATURE_FLAGS = [FeaturedFlags.FEATURE_FLAG_UVE_PREVIEW_MODE];

export const uveStoreMock = signalStore(
    withState<UVEState>(initialState),
    withFlags(MOCK_UVE_FEATURE_FLAGS)
);

const MOCK_RESPONSE = MOCK_UVE_FEATURE_FLAGS.reduce((acc, flag) => {
    acc[flag] = true;

    return acc;
}, {});

describe('withFlags', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            {
                provide: DotPropertiesService,
                useValue: {
                    getFeatureFlags: jest.fn().mockReturnValue(of(MOCK_RESPONSE))
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('onInit', () => {
        it('should call propertiesService.getFeatureFlags with flags', () => {
            const propertiesService = spectator.inject(DotPropertiesService);

            expect(propertiesService.getFeatureFlags).toHaveBeenCalledWith(MOCK_UVE_FEATURE_FLAGS);
        });

        it('should patch state with flags', () => {
            expect(store.flags()).toEqual(MOCK_RESPONSE);
        });
    });
    describe('methods', () => {
        describe('setFlags', () => {
            it('should patch state with flags', () => {
                store.setFlags(MOCK_RESPONSE);

                expect(store.flags()).toEqual(MOCK_RESPONSE);
            });
        });
    });
});
