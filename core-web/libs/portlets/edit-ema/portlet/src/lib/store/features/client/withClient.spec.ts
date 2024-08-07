import { describe } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';

import { ActivatedRoute, Router } from '@angular/router';

import { withClient } from './withClient';

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
    params: null,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true
};

const emptyParams = {} as DotPageApiParams;

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

    describe('withComputed', () => {
        it('should return initial configuration', () => {
            expect(store.$clientRequestProps()).toEqual({
                query: '',
                params: emptyParams
            });
        });

        it('should return the configuration from the main store', () => {
            const baseParams = {
                language_id: '1',
                url: 'http://example.com',
                'com.dotmarketing.persona.id': '2'
            };

            patchState(store, {
                params: baseParams
            });

            expect(store.$clientRequestProps()).toEqual({
                query: '',
                params: baseParams
            });
        });

        it('should return the configuration from the main store and the client', () => {
            const baseParams = {
                language_id: '1',
                url: 'http://example.com',
                'com.dotmarketing.persona.id': '2'
            };

            const clientParams = {
                'com.dotmarketing.persona.id': '3',
                depth: '2'
            };

            patchState(store, {
                params: baseParams,
                clientRequestProps: {
                    query: 'test',
                    params: clientParams
                }
            });

            expect(store.$clientRequestProps()).toEqual({
                query: 'test',
                params: {
                    ...baseParams,
                    ...clientParams
                }
            });
        });

        it('should return the query from the client', () => {
            const clientProps = {
                query: 'test',
                params: {}
            };

            patchState(store, {
                clientRequestProps: clientProps
            });

            expect(store.$clientRequestProps()).toEqual({
                query: 'test',
                params: emptyParams
            });
        });
    });

    describe('withMethods', () => {
        it('should set the client ready status', () => {
            store.setIsClientReady(true);

            expect(store.isClientReady()).toBe(true);
        });

        it('should set the client configuration', () => {
            const clientProps = {
                query: 'test',
                params: {}
            };

            store.setClientConfiguration(clientProps);

            expect(store.clientRequestProps()).toEqual(clientProps);
        });

        it('should reset the client configuration', () => {
            const clientProps = {
                query: 'test',
                params: {}
            };

            store.setClientConfiguration(clientProps);
            store.resetClientConfiguration();

            expect(store.clientRequestProps()).toEqual({
                query: '',
                params: {}
            });
        });
    });
});
