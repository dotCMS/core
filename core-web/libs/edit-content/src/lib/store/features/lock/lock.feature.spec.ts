/* eslint-disable @typescript-eslint/no-explicit-any */
import { patchState, signalStore, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService, SpyObject } from '@openng/spectator/vitest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, tick } from '@angular/core/testing';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotContentletCanLock,
    DotCurrentUser
} from '@dotcms/dotcms-models';

import { withLock } from './lock.feature';

import { initialRootState } from '../../edit-content.store';

describe('LockFeature', () => {
    let spectator: SpectatorService<any>;
    let store: any;
    let dotContentletService: SpyObject<DotContentletService>;
    let dotHttpErrorManagerService: SpyObject<DotHttpErrorManagerService>;
    let dotMessageService: SpyObject<DotMessageService>;

    const withTest = () =>
        signalStoreFeature(
            withState({
                ...initialRootState
            }),
            withMethods((store) => ({
                updateContent: (contentlet: DotCMSContentlet) => {
                    patchState(store, { contentlet });
                },
                updateCurrentUser: (currentUser: DotCurrentUser) => {
                    patchState(store, { currentUser });
                },
                updateCanLock: (canLock: boolean) => {
                    patchState(store, { canLock });
                }
            }))
        );

    const createStore = createServiceFactory({
        service: signalStore(withTest(), withLock()),
        mocks: [DotContentletService, DotHttpErrorManagerService, DotMessageService]
    });

    beforeEach(() => {
        spectator = createStore();
        store = spectator.service;
        dotContentletService = spectator.inject(DotContentletService);
        dotHttpErrorManagerService = spectator.inject(DotHttpErrorManagerService);
        dotMessageService = spectator.inject(DotMessageService);

        dotMessageService.get.mockImplementation((key, ...args) => {
            if (key === 'edit.content.locked.by.you') return 'You';
            if (key === 'edit.content.locked.by.user') return `Content is locked by ${args[0]}`;
            if (key === 'edit.content.locked.no.permission.user')
                return `Content is locked by ${args[0]}. You don't have permissions to unlock this content.`;
            if (key === 'edit.content.locked.no.permission')
                return "You don't have permissions to unlock this content.";

            return key;
        });
    });

    describe('computed properties', () => {
        it('should determine if content is locked', () => {
            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
            } as DotCMSContentlet);

            expect(store.isContentLocked()).toBe(true);

            store.updateContent({
                locked: false
            } as DotCMSContentlet);

            expect(store.isContentLocked()).toBe(false);
        });

        it('should generate correct lock warning message when locked by current user', () => {
            // Set current user
            store.updateCurrentUser({ userId: '123' });

            // Update with content locked by current user
            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
            });

            store.updateCanLock(true);

            expect(store.lockWarningMessage()).toBe(null);
        });

        it('should generate correct lock warning message when dont have lock permissions but the content is locked by other user', () => {
            // Set current user
            store.updateCurrentUser({ userId: '456' });

            // Update with content locked by another user
            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
            });

            expect(store.lockWarningMessage()).toEqual(
                "Content is locked by John Doe. You don't have permissions to unlock this content."
            );
        });

        it('should generate correct lock warning message when locked by other user', () => {
            // Set current user
            store.updateCurrentUser({ userId: '456' });
            // When user has permission to unlock
            store.updateCanLock(true);

            // Update with content locked by another user
            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
            });

            expect(store.lockWarningMessage()).toBe('Content is locked by John Doe');
        });

        it('should suppress the banner when firstName and lastName are both missing', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: null, lastName: null }
            });

            // No display name → don't render a misleading "Content locked by ." banner.
            expect(store.lockWarningMessage()).toBe(null);
        });

        it('should handle missing lastName gracefully', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: 'John', lastName: null }
            });

            expect(store.lockWarningMessage()).toBe('Content is locked by John');
        });

        it('should handle missing firstName gracefully', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: { userId: '123', firstName: null, lastName: 'Doe' }
            });

            expect(store.lockWarningMessage()).toBe('Content is locked by Doe');
        });

        it('should use lockedByName when lockedBy is a string (Page content type)', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: '123',
                lockedByName: 'Adrian Marquez'
            } as unknown as DotCMSContentlet);

            expect(store.lockWarningMessage()).toBe('Content is locked by Adrian Marquez');
        });

        it('should return null when lockedBy is a string matching the current user (Page locked by self)', () => {
            store.updateCurrentUser({ userId: '123' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: '123',
                lockedByName: 'Adrian Marquez'
            } as unknown as DotCMSContentlet);

            expect(store.lockWarningMessage()).toBe(null);
        });

        it('should suppress the banner when lockedBy is a string and lockedByName is missing', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: '123'
            } as unknown as DotCMSContentlet);

            // PageViewStrategy.getLockedByUserName can return null on DotSecurityException —
            // without a name, suppress the banner instead of rendering "Content locked by .".
            expect(store.lockWarningMessage()).toBe(null);
        });

        it('should suppress the banner when lockedBy is a string and lockedByName is blank/whitespace', () => {
            store.updateCurrentUser({ userId: '456' });
            store.updateCanLock(true);

            store.updateContent({
                locked: true,
                lockedBy: '123',
                lockedByName: '   '
            } as unknown as DotCMSContentlet);

            expect(store.lockWarningMessage()).toBe(null);
        });

        it('should return no-permission message when lockedBy is a string and user cannot unlock', () => {
            store.updateCurrentUser({ userId: '456' });
            // canLock stays false (default)

            store.updateContent({
                locked: true,
                lockedBy: '123',
                lockedByName: 'Adrian Marquez'
            } as unknown as DotCMSContentlet);

            expect(store.lockWarningMessage()).toBe(
                "Content is locked by Adrian Marquez. You don't have permissions to unlock this content."
            );
        });

        it('should fall back to the name-less no-permission key when locker has no display name', () => {
            store.updateCurrentUser({ userId: '456' });
            // canLock stays false (default)

            store.updateContent({
                locked: true,
                lockedBy: '123'
            } as unknown as DotCMSContentlet);

            expect(store.lockWarningMessage()).toBe(
                "You don't have permissions to unlock this content."
            );
        });

        it('should return empty message when content is not locked', () => {
            store.updateCanLock(true);
            store.updateContent({ locked: false });
            expect(store.lockWarningMessage()).toBe(null);
        });

        describe('isLockedByAnotherUser', () => {
            it('should be true when locked by a different user', () => {
                store.updateCurrentUser({ userId: '456' });
                store.updateContent({
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'Anna', lastName: 'García' }
                });

                expect(store.isLockedByAnotherUser()).toBe(true);
            });

            it('should be false when locked by the current user', () => {
                store.updateCurrentUser({ userId: '123' });
                store.updateContent({
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'Anna', lastName: 'García' }
                });

                expect(store.isLockedByAnotherUser()).toBe(false);
            });

            it('should be false when content is not locked', () => {
                store.updateCurrentUser({ userId: '456' });
                store.updateContent({ locked: false });

                expect(store.isLockedByAnotherUser()).toBe(false);
            });
        });

        describe('isReadOnly (R4, #38724)', () => {
            const ACTION = { id: 'publish', name: 'Publish' };

            /** A saved item the backend answered `canLock: false` and an empty action list for. */
            const readOnlyState = () => {
                // The onInit hook re-checks `_canlock` on every contentlet change.
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: false, locked: false } as DotContentletCanLock)
                );
                store.updateCurrentUser({ userId: '456' });
                patchState(store, {
                    contentlet: { inode: 'inode-1', locked: false } as DotCMSContentlet,
                    initialContentletState: 'existing',
                    canLock: false,
                    currentContentActions: {}
                });
            };

            it('should be true when the user cannot lock and has no workflow actions', () => {
                readOnlyState();

                expect(store.isReadOnly()).toBe(true);
            });

            it('should be false until the backend has answered the canLock check', () => {
                readOnlyState();
                patchState(store, { canLock: null });

                expect(store.isReadOnly()).toBe(false);
            });

            it('should be false when the user has workflow actions to run', () => {
                readOnlyState();
                patchState(store, { currentContentActions: { scheme: [ACTION] } as any });

                expect(store.isReadOnly()).toBe(false);
            });

            it('should be true when the content is locked by another user', () => {
                // `_canlock` and the action list answer the same for a READ-only user and an
                // EDIT user blocked by someone else's lock: canLock false, no actions. Neither
                // can save, so both get the disabled form (#38724 review).
                readOnlyState();
                store.updateContent({
                    inode: 'inode-1',
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
                } as unknown as DotCMSContentlet);

                expect(store.isReadOnly()).toBe(true);
            });

            it('should be false for new content', () => {
                readOnlyState();
                patchState(store, { initialContentletState: 'new' });

                expect(store.isReadOnly()).toBe(false);
            });
        });

        describe('isPermissionsSettled (no enabled-then-disabled flash, #38724 R4)', () => {
            it('should be false for an existing content until the backend answered _canlock', () => {
                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: false, locked: false } as DotContentletCanLock)
                );
                patchState(store, {
                    contentlet: { inode: 'inode-1', identifier: 'id-1' } as DotCMSContentlet,
                    canLock: null
                });

                expect(store.isPermissionsSettled()).toBe(false);

                patchState(store, { canLock: false });

                expect(store.isPermissionsSettled()).toBe(true);
            });

            it('should be true for new content, which has no lock to check', () => {
                patchState(store, { contentlet: null, canLock: null });

                expect(store.isPermissionsSettled()).toBe(true);
            });
        });

        describe('lockedByName', () => {
            it('should return the locker name when locked by another user', () => {
                store.updateCurrentUser({ userId: '456' });
                store.updateContent({
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'Anna', lastName: 'García' }
                });

                expect(store.lockedByName()).toBe('Anna García');
            });

            it('should return null when locked by the current user', () => {
                store.updateCurrentUser({ userId: '123' });
                store.updateContent({
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'Anna', lastName: 'García' }
                });

                expect(store.lockedByName()).toBeNull();
            });
        });
    });

    describe('methods', () => {
        describe('lockContent', () => {
            it('should lock content successfully', fakeAsync(() => {
                const mockContentlet = {
                    inode: '123',
                    locked: false
                } as DotCMSContentlet;

                const lockedContentlet = {
                    ...mockContentlet,
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
                };

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: true } as DotContentletCanLock)
                );

                store.updateContent(mockContentlet);
                dotContentletService.lockContent.mockReturnValue(of(lockedContentlet));

                store.lockContent();
                tick();

                expect(dotContentletService.lockContent).toHaveBeenCalledWith('123');
                expect(store.contentlet()).toEqual(lockedContentlet);
                expect(store.lockError()).toBeNull();
                expect(store.lockStatus()).toBe(ComponentStatus.LOADED);
                expect(store.isLocking()).toBe(false);
            }));

            it('should handle error when locking content', fakeAsync(() => {
                const mockContentlet = {
                    inode: '123',
                    locked: false
                } as DotCMSContentlet;

                const mockError = new HttpErrorResponse({
                    status: 400,
                    statusText: 'Bad Request'
                });

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: false } as DotContentletCanLock)
                );

                store.updateContent(mockContentlet);
                dotContentletService.lockContent.mockReturnValue(throwError(() => mockError));

                store.lockContent();
                tick();

                expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
                expect(store.lockStatus()).toBe(ComponentStatus.ERROR);
                expect(store.isLocking()).toBe(false);
                //expect(store.lockError()).toBe(mockError.error.message);
            }));
        });

        describe('unlockContent', () => {
            it('should unlock content successfully', fakeAsync(() => {
                const mockContentlet = {
                    inode: '123',
                    locked: true,
                    lockedBy: { userId: '123', firstName: 'John', lastName: 'Doe' }
                } as DotCMSContentlet;

                const unlockedContentlet = {
                    ...mockContentlet,
                    locked: false,
                    lockedBy: null
                };

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: true } as DotContentletCanLock)
                );

                store.updateContent(mockContentlet);
                dotContentletService.unlockContent.mockReturnValue(of(unlockedContentlet));

                store.unlockContent();
                tick();

                expect(dotContentletService.unlockContent).toHaveBeenCalledWith('123');
                expect(store.contentlet()).toEqual(unlockedContentlet);
                expect(store.lockError()).toBeNull();
                expect(store.lockStatus()).toBe(ComponentStatus.LOADED);
                expect(store.isLocking()).toBe(false);
            }));

            it('should handle error when unlocking content', fakeAsync(() => {
                const mockContentlet = {
                    inode: '123',
                    locked: true
                } as DotCMSContentlet;

                const mockError = new HttpErrorResponse({ status: 400, statusText: 'Bad Request' });

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: true } as DotContentletCanLock)
                );

                store.updateContent(mockContentlet);
                dotContentletService.unlockContent.mockReturnValue(throwError(() => mockError));

                store.unlockContent();
                tick();

                expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
                expect(store.lockStatus()).toBe(ComponentStatus.ERROR);
                expect(store.isLocking()).toBe(false);
                //expect(store.lockError()).toBe(mockError.message);
            }));
        });

        describe('checkCanLock', () => {
            it('should update canLock state when content can be locked', fakeAsync(() => {
                const mockContentlet = { inode: '123' } as DotCMSContentlet;
                store.updateContent(mockContentlet);

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: false } as DotContentletCanLock)
                );

                store.checkCanLock();
                tick();

                expect(dotContentletService.canLock).toHaveBeenCalledWith('123');
                expect(store.canLock()).toBe(true);
                expect(store.lockSwitchLabel()).toBe('edit.content.unlocked');
            }));

            it('should update canLock state when content is already locked', fakeAsync(() => {
                const mockContentlet = { inode: '123' } as DotCMSContentlet;
                store.updateContent(mockContentlet);

                dotContentletService.canLock.mockReturnValue(
                    of({ canLock: true, locked: true } as DotContentletCanLock)
                );

                store.checkCanLock();
                tick();

                expect(store.canLock()).toBe(true);
                expect(store.lockSwitchLabel()).toBe('edit.content.locked');
            }));

            it('should handle error when checking if content can be locked', fakeAsync(() => {
                const mockContentlet = { inode: '123' } as DotCMSContentlet;
                store.updateContent(mockContentlet);

                const mockError = new HttpErrorResponse({ status: 400, statusText: 'Bad Request' });
                dotContentletService.canLock.mockReturnValue(throwError(() => mockError));

                store.checkCanLock();
                tick();

                expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
                expect(store.canLock()).toBe(false);
            }));

            it('should ignore a reply that arrives after another contentlet replaced the one it asked about (#38724 review)', fakeAsync(() => {
                // Item A's slow `_canlock` must not settle item B's permission.
                const reply = new Subject<DotContentletCanLock>();
                dotContentletService.canLock.mockReturnValue(reply);
                patchState(store, { contentlet: { inode: 'inode-a' } as DotCMSContentlet });
                store.checkCanLock();

                patchState(store, {
                    contentlet: { inode: 'inode-b' } as DotCMSContentlet,
                    canLock: null
                });
                reply.next({ canLock: true, locked: false } as DotContentletCanLock);
                tick();

                expect(store.canLock()).toBeNull();
            }));

            it('should set canLock to false when no contentlet exists', fakeAsync(() => {
                store.updateContent(null);
                store.checkCanLock();
                tick();

                expect(dotContentletService.canLock).not.toHaveBeenCalled();
                expect(store.canLock()).toBe(false);
                expect(store.lockSwitchLabel()).toBe('edit.content.unlocked');
            }));
        });
    });

    describe('hooks', () => {
        it('should check if content can be locked when contentlet changes', fakeAsync(() => {
            const mockContentlet = { inode: '123' } as DotCMSContentlet;
            dotContentletService.canLock.mockReturnValue(
                of({ canLock: true, locked: false } as DotContentletCanLock)
            );

            // Trigger the effect by updating the contentlet
            store.updateContent(mockContentlet);

            spectator.flushEffects();

            expect(dotContentletService.canLock).toHaveBeenCalledWith('123');
            expect(store.canLock()).toBe(true);
        }));
    });
});
