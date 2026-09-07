import { describe } from '@jest/globals';
import { signalStore } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

import { withFlags } from './with-flags.feature';

const FLAG = FeaturedFlags.FEATURE_FLAG_UVE_PREVIEW_MODE;
const MOCK_FLAGS = [FLAG];

// Throwaway host store — withFlags declares no host-state constraint, so it composes bare.
const flagsStore = signalStore(withFlags(MOCK_FLAGS));

describe('withFlags', () => {
    describe('onInit', () => {
        let spectator: SpectatorService<InstanceType<typeof flagsStore>>;
        let store: InstanceType<typeof flagsStore>;

        const createService = createServiceFactory({
            service: flagsStore,
            providers: [
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getFeatureFlags: jest.fn().mockReturnValue(of({ [FLAG]: true }))
                    }
                }
            ]
        });

        beforeEach(() => {
            spectator = createService();
            store = spectator.service;
        });

        it('calls getFeatureFlags with the requested flags', () => {
            expect(spectator.inject(DotPropertiesService).getFeatureFlags).toHaveBeenCalledWith(
                MOCK_FLAGS
            );
        });

        it('patches the resolved flags into state', () => {
            expect(store.flags()).toEqual({ [FLAG]: true });
        });
    });

    describe('normalization', () => {
        const propertiesServiceMock = { getFeatureFlags: jest.fn() };
        const createService = createServiceFactory({
            service: flagsStore,
            providers: [{ provide: DotPropertiesService, useValue: propertiesServiceMock }]
        });

        it('normalizes NOT_FOUND to true (flag not configured on server → enabled by default)', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(
                of({ [FLAG]: FEATURE_FLAG_NOT_FOUND })
            );
            expect(createService().service.flags()[FLAG]).toBe(true);
        });

        it('keeps boolean true as true', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(of({ [FLAG]: true }));
            expect(createService().service.flags()[FLAG]).toBe(true);
        });

        it('keeps boolean false as false', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(of({ [FLAG]: false }));
            expect(createService().service.flags()[FLAG]).toBe(false);
        });

        it('normalizes any unknown string value to false', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(
                of({ [FLAG]: 'FF_NOT_AVAILABLE' })
            );
            expect(createService().service.flags()[FLAG]).toBe(false);
        });
    });

    describe('error handling', () => {
        const propertiesServiceMock = { getFeatureFlags: jest.fn() };
        const createService = createServiceFactory({
            service: flagsStore,
            providers: [{ provide: DotPropertiesService, useValue: propertiesServiceMock }]
        });

        it('degrades to no enabled flags when the config read fails (never throws)', () => {
            propertiesServiceMock.getFeatureFlags.mockReturnValue(
                throwError(() => new Error('config unavailable'))
            );

            const s = createService();

            expect(() => s.service.flags()).not.toThrow();
            expect(s.service.flags()).toEqual({});
            expect(s.service.flags()[FLAG]).toBeUndefined();
        });
    });
});
