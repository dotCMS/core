import { describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import { withUVEToolbar } from './withUVEToolbar';

import { DotPageApiService } from '../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { UVE_STATUS } from '../../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../../shared/mocks';
import { UVEState } from '../../../models';

const pageParams = {
    url: 'test-url',
    language_id: '1',
    'com.dotmarketing.persona.id': 'dot:persona',
    variantName: 'DEFAULT',
    clientHost: 'http://localhost:3000',
    mode: 'EDIT_MODE'
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
    isClientReady: false
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
    });
});
