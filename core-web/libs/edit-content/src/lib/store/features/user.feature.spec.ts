/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import { DotCurrentUserService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCurrentUser } from '@dotcms/dotcms-models';

import { withUser } from './user.feature';

describe('UserFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let dotCurrentUserService: SpyObject<DotCurrentUserService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createStore = createServiceFactory({
        service: signalStore(withUser()),
        mocks: [DotCurrentUserService, DotHttpErrorManagerService]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotCurrentUserService = spectator.inject(DotCurrentUserService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
    });

    describe('loadCurrentUser', () => {
        it('should load current user successfully', fakeAsync(() => {
            const mockUser: DotCurrentUser = {
                userId: '123',
                givenName: 'John',
                surnaname: 'Doe',
                email: 'john.doe@example.com',
                roleId: 'admin',
                admin: true
            };

            dotCurrentUserService.getCurrentUser.mockReturnValue(of(mockUser));

            store.loadCurrentUser();
            tick();

            expect(dotCurrentUserService.getCurrentUser).toHaveBeenCalled();
            expect(store.currentUser()).toEqual(mockUser);
        }));

        it('should handle error when loading current user', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
            dotCurrentUserService.getCurrentUser.mockReturnValue(throwError(() => mockError));

            store.loadCurrentUser();
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockError);
            expect(store.currentUser()).toBeNull();
        }));
    });
});
