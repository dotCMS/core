import { signalStore, withState } from '@ngrx/signals';

import { TestBed } from '@angular/core/testing';

import { CurrentUser } from '@dotcms/dotcms-js';

import { withUve } from './withUve';

import { UVE_STATUS } from '../../../shared/enums';

describe('withUve', () => {
    const TestStore = signalStore(withState({}), withUve());

    let store: InstanceType<typeof TestStore>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TestStore]
        });
        store = TestBed.inject(TestStore);
    });

    describe('Initial State', () => {
        it('should initialize with LOADING status', () => {
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADING);
        });

        it('should initialize with isEnterprise as false', () => {
            expect(store.uveIsEnterprise()).toBe(false);
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

        it('should allow multiple status changes', () => {
            store.setUveStatus(UVE_STATUS.LOADING);
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADING);

            store.setUveStatus(UVE_STATUS.LOADED);
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);

            store.setUveStatus(UVE_STATUS.ERROR);
            expect(store.uveStatus()).toBe(UVE_STATUS.ERROR);
        });
    });

    describe('setUveIsEnterprise', () => {
        it('should set enterprise flag to true', () => {
            store.setUveIsEnterprise(true);
            expect(store.uveIsEnterprise()).toBe(true);
        });

        it('should set enterprise flag to false', () => {
            store.setUveIsEnterprise(true);
            store.setUveIsEnterprise(false);
            expect(store.uveIsEnterprise()).toBe(false);
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

        it('should allow clearing current user', () => {
            store.setUveCurrentUser(mockUser);
            store.setUveCurrentUser(null);
            expect(store.uveCurrentUser()).toBeNull();
        });

        it('should allow updating to different user', () => {
            const mockUser2: CurrentUser = {
                userId: 'user456',
                email: 'another@example.com',
                givenName: 'Another',
                surname: 'User',
                roleId: 'role456'
            } as CurrentUser;

            store.setUveCurrentUser(mockUser);
            expect(store.uveCurrentUser()).toEqual(mockUser);

            store.setUveCurrentUser(mockUser2);
            expect(store.uveCurrentUser()).toEqual(mockUser2);
        });
    });

    describe('Integration scenarios', () => {
        it('should support typical initialization flow', () => {
            // Start with loading
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADING);

            // Set user and enterprise flag during load
            const mockUser: CurrentUser = {
                userId: 'user123',
                email: 'test@example.com'
            } as CurrentUser;

            store.setUveCurrentUser(mockUser);
            store.setUveIsEnterprise(true);

            // Complete loading
            store.setUveStatus(UVE_STATUS.LOADED);

            // Verify final state
            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
            expect(store.uveIsEnterprise()).toBe(true);
            expect(store.uveCurrentUser()).toEqual(mockUser);
        });

        it('should support error recovery flow', () => {
            const mockUser: CurrentUser = {
                userId: 'user123',
                email: 'test@example.com'
            } as CurrentUser;

            // Set user and enterprise
            store.setUveCurrentUser(mockUser);
            store.setUveIsEnterprise(true);

            // Error occurs
            store.setUveStatus(UVE_STATUS.ERROR);
            expect(store.uveStatus()).toBe(UVE_STATUS.ERROR);

            // User and enterprise state persists through error
            expect(store.uveCurrentUser()).toEqual(mockUser);
            expect(store.uveIsEnterprise()).toBe(true);

            // Recovery - retry load
            store.setUveStatus(UVE_STATUS.LOADING);
            store.setUveStatus(UVE_STATUS.LOADED);

            expect(store.uveStatus()).toBe(UVE_STATUS.LOADED);
        });
    });
});
