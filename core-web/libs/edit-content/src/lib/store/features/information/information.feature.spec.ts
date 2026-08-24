/* eslint-disable @typescript-eslint/no-explicit-any */
import { patchState, signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService, SpyObject } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { HttpCode } from '@dotcms/dotcms-js';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';

import { withInformation } from './information.feature';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
import { initialRootState } from '../../edit-content.store';

describe('Information Feature Store', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let dotEditContentService: SpyObject<DotEditContentService>;

    const mockContentlet = {
        identifier: 'test-identifier',
        inode: 'test-inode',
        languageId: 1
    } as DotCMSContentlet;

    const createStore = createServiceFactory({
        service: signalStore(withState(initialRootState), withInformation()),
        mocks: [DotEditContentService, DotHttpErrorManagerService]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotEditContentService = spectator.inject(DotEditContentService);
    });

    describe('isLoadingInformation', () => {
        it('should be true only while the reference pages request is in flight', fakeAsync(() => {
            dotEditContentService.getReferencePages.mockReturnValue(of(3).pipe(delay(100)));

            expect(store.isLoadingInformation()).toBe(false);

            store.getReferencePages('test-identifier');
            expect(store.isLoadingInformation()).toBe(true);

            tick(100);
            expect(store.isLoadingInformation()).toBe(false);
        }));
    });

    describe('getReferencePages', () => {
        it('should set loading state and update relatedContent on success', fakeAsync(() => {
            dotEditContentService.getReferencePages.mockReturnValue(of(3).pipe(delay(100)));

            store.getReferencePages('test-identifier');

            expect(store.information()).toEqual({
                status: ComponentStatus.LOADING,
                error: null,
                relatedContent: '0'
            });

            tick(100);

            expect(store.information()).toEqual({
                status: ComponentStatus.LOADED,
                error: null,
                relatedContent: '3'
            });
        }));

        it('should handle errors', fakeAsync(() => {
            const httpError = new HttpErrorResponse({
                status: HttpCode.SERVER_ERROR,
                statusText: 'Server Error',
                error: { message: 'Backend error message' }
            });
            dotEditContentService.getReferencePages.mockReturnValue(throwError(() => httpError));

            store.getReferencePages('test-identifier');
            tick();

            expect(store.information().status).toBe(ComponentStatus.ERROR);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
        }));
    });

    describe('Automatic Reference Pages Loading Effect', () => {
        beforeEach(() => {
            dotEditContentService.getReferencePages.mockReturnValue(of(2));
        });

        it('should automatically load reference pages when the contentlet is set', fakeAsync(() => {
            patchState(store, { contentlet: mockContentlet });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getReferencePages).toHaveBeenCalledWith('test-identifier');
            expect(store.information().relatedContent).toBe('2');
        }));

        it('should automatically reload when a save mints a new inode under the same identifier', fakeAsync(() => {
            patchState(store, { contentlet: mockContentlet });
            spectator.flushEffects();
            tick();
            dotEditContentService.getReferencePages.mockClear();

            patchState(store, { contentlet: { ...mockContentlet, inode: 'new-inode-after-save' } });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getReferencePages).toHaveBeenCalledWith('test-identifier');
        }));

        it('should not load reference pages when the sidebar is closed', fakeAsync(() => {
            patchState(store, {
                contentlet: mockContentlet,
                uiState: { ...store.uiState(), isSidebarOpen: false }
            });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getReferencePages).not.toHaveBeenCalled();
        }));

        it('should not load reference pages when there is no contentlet identifier', fakeAsync(() => {
            patchState(store, { contentlet: null });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getReferencePages).not.toHaveBeenCalled();
        }));

        it('should not reload reference pages on unrelated uiState changes', fakeAsync(() => {
            patchState(store, { contentlet: mockContentlet });
            spectator.flushEffects();
            tick();
            dotEditContentService.getReferencePages.mockClear();

            // Every uiState writer replaces the slice wholesale, so the effect must
            // depend on the isSidebarOpen leaf rather than the object.
            patchState(store, {
                uiState: { ...store.uiState(), activeSidebarTab: 2, view: 'form' }
            });
            spectator.flushEffects();
            tick();

            expect(dotEditContentService.getReferencePages).not.toHaveBeenCalled();
        }));
    });
});
