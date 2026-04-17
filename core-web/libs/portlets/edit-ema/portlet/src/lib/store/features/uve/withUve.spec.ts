import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';

import { signal } from '@angular/core';

import { CurrentUser } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

import { withUve } from './withUve';

import { UVE_STATUS } from '../../../shared/enums';

const mockUser: CurrentUser = {
    userId: 'user123',
    email: 'test@example.com',
    givenName: 'Test',
    surname: 'User',
    roleId: 'role123',
    admin: false,
    loginAs: false
};
const loggedUserSignal = signal<CurrentUser | null>(mockUser);
const TestStore = signalStore(withState({}), withUve());

describe('withUve', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [
            {
                provide: GlobalStore,
                useValue: { loggedUser: loggedUserSignal }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        spectator.flushEffects(); // Run onInit effect so uveCurrentUser syncs from GlobalStore.loggedUser
    });

    describe('Initial State', () => {
        it('should initialize with LOADING status', () => {
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADING);
        });

        it('should initialize with a valid currentUser', () => {
            expect(store.uveCurrentUser()).toBe(mockUser);
        });

        it('should sync uveCurrentUser from GlobalStore.loggedUser when store is created', () => {
            expect(store.uveCurrentUser()).toBe(mockUser);

            loggedUserSignal.set(null);
            spectator.flushEffects();
            expect(store.uveCurrentUser()).toBeNull();
        });
    });

    describe('setUveStatus', () => {
        it('should update status to LOADED', () => {
            store.setUveStatus(UVE_STATUS.LOADED);
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });

        it('should update status to ERROR', () => {
            store.setUveStatus(UVE_STATUS.ERROR);
            expect(store.uveStatus()).toBe(UVE_STATUS.ERROR);
        });
    });

    describe('setUveCurrentUser', () => {
        const mockUser: CurrentUser = {
            userId: 'user123',
            email: 'test@example.com',
            givenName: 'Test',
            surname: 'User',
            roleId: 'role123'
        } as CurrentUser;

        it('should set current user', () => {
            store.setUveCurrentUser(mockUser);
            expect(store.uveCurrentUser()).toEqual(mockUser);
        });
    });
});
