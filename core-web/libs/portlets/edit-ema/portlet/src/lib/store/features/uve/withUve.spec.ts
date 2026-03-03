import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { CurrentUser } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';

import { withUve } from './withUve';

import { UVE_STATUS } from '../../../shared/enums';

describe('withUve', () => {
    const TestStore = signalStore(withState({}), withUve());

    let store: InstanceType<typeof TestStore>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TestStore, { provide: GlobalStore, useValue: { loggedUser: () => null } }]
        });
        store = TestBed.inject(TestStore);
    });

    describe('Initial State', () => {
        it('should initialize with LOADING status', () => {
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADING);
        });

        it('should initialize with null currentUser', () => {
            expect(store.uveCurrentUser()).toBeNull();
        });

        it('should have $isCMSAdmin false when no user', () => {
            expect(store.$isCMSAdmin()).toBe(false);
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

        it('should update $isCMSAdmin when user has admin flag', () => {
            expect(store.$isCMSAdmin()).toBe(false);

            store.setUveCurrentUser({ ...mockUser, admin: true } as CurrentUser);
            expect(store.$isCMSAdmin()).toBe(true);

            store.setUveCurrentUser({ ...mockUser, admin: false } as CurrentUser);
            expect(store.$isCMSAdmin()).toBe(false);
        });
    });
});
