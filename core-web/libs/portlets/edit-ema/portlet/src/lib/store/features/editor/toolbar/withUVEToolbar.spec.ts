import { describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { mockDotDevices } from '@dotcms/utils-testing';

import { withUVEToolbar } from './withUVEToolbar';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../../shared/mocks';
import { Orientation, UVEState } from '../../../models';

const pageParams = {
    url: 'test-url',
    language_id: '1',
    'com.dotmarketing.persona.id': 'dot:persona',
    variantName: 'DEFAULT',
    clientHost: 'http://localhost:3000'
};

const initialState: UVEState = {
    isEnterprise: true,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADED,
    isTraditionalPage: false,
    canEditPage: true,
    pageIsLocked: true,
    isClientReady: false,
    viewParams: {
        orientation: undefined,
        seo: undefined,
        device: undefined
    }
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withUVEToolbar());

describe('withEditor', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(Router),
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of(MOCK_RESPONSE_HEADLESS);
                    },
                    getClientPage() {
                        return of(MOCK_RESPONSE_HEADLESS);
                    },
                    save: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Computed', () => {
        it('should return the right API URL', () => {
            const params = { ...pageParams };
            // Delete the url from the params to test the function
            delete params.url;

            const queryParams = new URLSearchParams(params).toString();
            const expectURL = `/api/v1/page/json/test-url?${queryParams}`;

            expect(store.$apiURL()).toBe(expectURL);
        });

        it('should return the personaSelector props', () => {
            expect(store.$personaSelector()).toEqual({
                pageId: '123',
                value: DEFAULT_PERSONA
            });
        });

        // We don't need to test all the interactions of this because we already test this in the proper component
        it('should return the right infoDisplayProps', () => {
            expect(store.$infoDisplayProps()).toEqual(null);
        });
    });

    describe('methods', () => {
        describe('setDevice', () => {
            it('should set the device and viewParams', () => {
                const iphone = mockDotDevices[0];

                store.setDevice(iphone);
                expect(store.device()).toBe(iphone);
                expect(store.orientation()).toBe(Orientation.LANDSCAPE); // This mock is on landscape, because the width is greater than the height

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.LANDSCAPE,
                    seo: undefined
                });
            });

            it('should set the device and orientation', () => {
                const iphone = mockDotDevices[0];

                store.setDevice(iphone, Orientation.PORTRAIT);
                expect(store.device()).toBe(iphone);
                expect(store.orientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: iphone.inode,
                    orientation: Orientation.PORTRAIT,
                    seo: undefined
                });
            });
        });

        describe('setOrientation', () => {
            it('should set the orientation and the view params', () => {
                store.setOrientation(Orientation.PORTRAIT);
                expect(store.orientation()).toBe(Orientation.PORTRAIT);

                expect(store.viewParams()).toEqual({
                    device: store.viewParams().device,
                    orientation: Orientation.PORTRAIT,
                    seo: undefined
                });
            });

            describe('clearDeviceAndSocialMedia', () => {
                // We have to extend this test because we have to test the social media
                it('should clear the device and social media', () => {
                    store.setDevice(mockDotDevices[0]);

                    store.clearDeviceAndSocialMedia();

                    expect(store.device()).toBe(null);
                    expect(store.socialMedia()).toBe(null);
                    expect(store.isEditState()).toBe(true);
                    expect(store.orientation()).toBe(null);
                    expect(store.viewParams()).toEqual({
                        device: undefined,
                        orientation: undefined,
                        seo: undefined
                    });
                });
            });
        });
    });
});
