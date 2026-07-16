import { describe } from '@jest/globals';
import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

import { withFlags } from './withFlags';

import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';

const initialState = createInitialUVEState();

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
    describe('onInit', () => {
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

        it('should call propertiesService.getFeatureFlags with flags', () => {
            const propertiesService = spectator.inject(DotPropertiesService);

            expect(propertiesService.getFeatureFlags).toHaveBeenCalledWith(MOCK_UVE_FEATURE_FLAGS);
        });

        it('should patch state with flags', () => {
            expect(store.flags()).toEqual(MOCK_RESPONSE);
        });
    });

    describe('flag normalization', () => {
        const flag = FeaturedFlags.FEATURE_FLAG_UVE_PREVIEW_MODE;
        const propertiesServiceMock = { getFeatureFlags: jest.fn() };

        const createService = createServiceFactory({
            service: uveStoreMock,
            providers: [{ provide: DotPropertiesService, useValue: propertiesServiceMock }]
        });

        it('should normalize NOT_FOUND to true (flag not configured on server)', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(
                of({ [flag]: FEATURE_FLAG_NOT_FOUND })
            );
            const s = createService();
            expect(s.service.flags()[flag]).toBe(true);
        });

        it('should keep boolean true as true (flag explicitly enabled)', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(of({ [flag]: true }));
            const s = createService();
            expect(s.service.flags()[flag]).toBe(true);
        });

        it('should keep boolean false as false (flag explicitly disabled)', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(of({ [flag]: false }));
            const s = createService();
            expect(s.service.flags()[flag]).toBe(false);
        });

        it('should normalize any unknown string value to false', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(
                of({ [flag]: 'FF_NOT_AVAILABLE' })
            );
            const s = createService();
            expect(s.service.flags()[flag]).toBe(false);
        });
    });
});
