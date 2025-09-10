/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import { DotCurrentUserService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { CurrentUserDataMock } from '@dotcms/utils-testing';

import { withUser } from './user.feature';

import { initialRootState } from '../../edit-content.store';

describe('UserFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let dotCurrentUserService: SpyObject<DotCurrentUserService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;

    const createStore = createServiceFactory({
        service: signalStore(withState({ ...initialRootState }), withUser()),
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
            dotCurrentUserService.getCurrentUser.mockReturnValue(of(CurrentUserDataMock));

            store.loadCurrentUser();
            tick();

            expect(dotCurrentUserService.getCurrentUser).toHaveBeenCalled();
            expect(store.currentUser()).toEqual(CurrentUserDataMock);
        }));

        it('should handle error when loading current user', fakeAsync(() => {
            const mockError = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
            dotCurrentUserService.getCurrentUser.mockReturnValue(throwError(() => mockError));

            store.loadCurrentUser();
            tick();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
            expect(store.currentUser()).toBeNull();
        }));
    });
});
