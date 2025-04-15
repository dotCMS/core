/* eslint-disable @typescript-eslint/no-explicit-any */
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import { MessageService } from 'primeng/api';

import { delay } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { withActivities } from './activities.feature';

import { Activity } from '../../../models/dot-edit-content.model';
import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { initialRootState } from '../../edit-content.store';

describe('Activities Feature Store', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let errorManager: SpyObject<DotHttpErrorManagerService>;
    let dotEditContentService: SpyObject<DotEditContentService>;
    let messageService: SpyObject<MessageService>;
    let dotMessageService: SpyObject<DotMessageService>;

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
        mocks: [
            DotEditContentService,
            DotHttpErrorManagerService,
            MessageService,
            DotMessageService
        ]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        errorManager = spectator.inject(DotHttpErrorManagerService);
        dotEditContentService = spectator.inject(DotEditContentService);
        messageService = spectator.inject(MessageService);
        dotMessageService = spectator.inject(DotMessageService);
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

    describe('addComment', () => {
        const mockComment = 'Test comment';
        const mockNewActivity: Activity = {
            commentDescription: mockComment,
            createdDate: Date.now(),
            email: 'test@test.com',
            postedBy: 'Test User',
            roleId: '1',
            taskId: '1',
            type: 'comment'
        };

        beforeEach(() => {
            dotMessageService.get.mockImplementation((key) => key);
        });

        it('should set saving state when starting to add comment', fakeAsync(() => {
            // Arrange
            dotEditContentService.createActivity.mockReturnValue(
                of(mockNewActivity).pipe(delay(100))
            );

            // Act
            store.addComment({ identifier: mockIdentifier, comment: mockComment });

            // Assert - Check saving state before tick
            expect(store.activitiesStatus()).toEqual({
                status: ComponentStatus.SAVING,
                error: null
            });

            tick(100);
        }));

        it('should update state and show success message when comment is added successfully', fakeAsync(() => {
            // Arrange
            dotEditContentService.createActivity.mockReturnValue(
                of(mockNewActivity).pipe(delay(100))
            );

            // Act
            store.addComment({ identifier: mockIdentifier, comment: mockComment });
            tick(100);

            // Assert
            expect(messageService.clear).toHaveBeenCalled();
            expect(messageService.add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'edit.content.sidebar.activities.comment.success.title',
                detail: 'edit.content.sidebar.activities.comment.success.message'
            });

            const activities = store.activities();
            expect(activities.some((activity) => activity.commentDescription === mockComment)).toBe(
                true
            );
            expect(store.activitiesStatus()).toEqual({
                status: ComponentStatus.IDLE,
                error: null
            });
        }));

        it('should call createActivity with correct parameters', fakeAsync(() => {
            // Arrange
            dotEditContentService.createActivity.mockReturnValue(
                of(mockNewActivity).pipe(delay(100))
            );

            // Act
            store.addComment({ identifier: mockIdentifier, comment: mockComment });
            tick(100);

            // Assert
            expect(dotEditContentService.createActivity).toHaveBeenCalledWith(
                mockIdentifier,
                mockComment
            );
        }));

        it('should handle error when adding comment fails', fakeAsync(() => {
            // Arrange
            const handleErrorSpy = jest.spyOn(errorManager, 'handle');
            const httpError = new HttpErrorResponse({
                status: HttpCode.SERVER_ERROR,
                statusText: 'Server Error',
                error: {
                    message: 'Backend error message'
                }
            });
            dotEditContentService.createActivity.mockReturnValue(throwError(() => httpError));

            // Act
            store.addComment({ identifier: mockIdentifier, comment: mockComment });
            tick();

            // Assert
            expect(handleErrorSpy).toHaveBeenCalledTimes(1);
        }));

        it('should append new activity to existing activities', fakeAsync(() => {
            // Arrange
            // First load existing activities
            dotEditContentService.getActivities.mockReturnValue(of(mockActivities));
            store.loadActivities(mockIdentifier);
            tick();

            // Setup for adding new comment
            dotEditContentService.createActivity.mockReturnValue(
                of(mockNewActivity).pipe(delay(100))
            );

            // Act
            store.addComment({ identifier: mockIdentifier, comment: mockComment });
            tick(100);

            // Assert
            expect(store.activities()).toEqual([...mockActivities, mockNewActivity]);
        }));
    });
});
