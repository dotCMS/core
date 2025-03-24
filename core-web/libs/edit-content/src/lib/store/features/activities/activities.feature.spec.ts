import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { HttpCode } from '@dotcms/dotcms-js';
import { Activity } from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { initialRootState } from '../../edit-content.store';
import { withActivities } from './activities.feature';

describe('Activities Feature Store', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let errorManager: SpyObject<DotHttpErrorManagerService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const mockActivities: Activity[] = [
        {
            commentDescription: 'Test comment 1',
            createdDate: Date.now(),
            email: 'test@test.com',
            postedBy: 'Test User',
            roleId: '1',
            taskId: '1',
            type: 'comment'
        }
    ];

    const mockIdentifier = {
        id: '123',
        lang: 1
    };

    const createStore = createServiceFactory({
        service: signalStore(withState(initialRootState), withActivities()),
        mocks: [DotEditContentService, DotHttpErrorManagerService]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        errorManager = spectator.inject(DotHttpErrorManagerService);
        dotEditContentService = spectator.inject(DotEditContentService);
    });

    describe('loadActivities', () => {
        it('should set loading state when starting to load activities', fakeAsync(() => {
            // Arrange
            dotEditContentService.getActivities.mockReturnValue(
                of(mockActivities).pipe(delay(100))
            );

            // Act
            store.loadActivities(mockIdentifier);

            // Assert - Check loading state before tick
            expect(store.activitiesStatus()).toEqual({
                status: ComponentStatus.LOADING,
                error: null
            });

            // Complete the async operation
            tick(100);

            // Verify final state
            expect(store.activitiesStatus()).toEqual({
                status: ComponentStatus.LOADED,
                error: null
            });
        }));

        it('should update state with activities when load is successful', fakeAsync(() => {
            // Arrange
            dotEditContentService.getActivities.mockReturnValue(
                of(mockActivities).pipe(delay(100))
            );

            // Act
            store.loadActivities(mockIdentifier);
            tick(100);

            // Assert
            expect(store.activities()).toEqual(mockActivities);
            expect(store.activitiesStatus()).toEqual({
                status: ComponentStatus.LOADED,
                error: null
            });
        }));

        it('should call getActivities with correct identifier', fakeAsync(() => {
            // Arrange
            dotEditContentService.getActivities.mockReturnValue(
                of(mockActivities).pipe(delay(100))
            );

            // Act
            store.loadActivities(mockIdentifier);
            tick(100);

            // Assert
            expect(dotEditContentService.getActivities).toHaveBeenCalledWith(mockIdentifier);
        }));
    });

    describe('loadActivities error handling', () => {
        it('should handle error when loading activities fails', fakeAsync(() => {
            const handleErrorSpy = jest.spyOn(errorManager, 'handle');
            // Arrange
            const httpError = new HttpErrorResponse({
                status: HttpCode.SERVER_ERROR,
                statusText: 'Server Error',
                error: {
                    message: 'Backend error message'
                }
            });
            dotEditContentService.getActivities.mockReturnValue(throwError(() => httpError));

            // Act
            store.loadActivities(mockIdentifier);
            tick();

            // Assert
            expect(handleErrorSpy).toHaveBeenCalledTimes(1);
        }));
    });
});
