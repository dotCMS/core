import { describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { ActivatedRoute, Router } from '@angular/router';

import { withClient } from './withClient';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { UVEState } from '../../models';

const emptyParams = {} as DotPageApiParams;

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    params: emptyParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withClient());

describe('UVEStore', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [mockProvider(Router), mockProvider(ActivatedRoute)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should have initial state', () => {
        expect(store.isClientReady()).toBeFalsy();
        expect(store.clientRequestProps()).toEqual({
            params: null,
            query: ''
        });
    });

    describe('withMethods', () => {
        it('should set the client ready status', () => {
            store.setIsClientReady(true);

            expect(store.isClientReady()).toBe(true);
        });

        describe('setClientConfiguration', () => {
            it('should set the client configuration', () => {
                const clientProps = {
                    query: 'test',
                    params: {
                        depth: '1'
                    }
                };

                store.setClientConfiguration(clientProps);

                expect(store.clientRequestProps()).toEqual(clientProps);
            });
        });

        it('should reset the client configuration', () => {
            const clientProps = {
                query: 'test',
                params: null
            };

            store.setClientConfiguration(clientProps);
            store.resetClientConfiguration();

            expect(store.clientRequestProps()).toEqual({
                query: '',
                params: null
            });
        });
    });
});
