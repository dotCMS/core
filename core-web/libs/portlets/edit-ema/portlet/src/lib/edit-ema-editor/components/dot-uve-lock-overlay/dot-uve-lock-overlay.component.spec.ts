import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUveLockOverlayComponent } from './dot-uve-lock-overlay.component';

import { ToggleLockOptions } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';

describe('DotUveLockOverlayComponent', () => {
    let spectator: Spectator<DotUveLockOverlayComponent>;

    const mockToggleLockOptions = signal<ToggleLockOptions>({
        inode: 'test-inode',
        isLocked: false,
        lockedBy: '',
        canLock: true,
        isLockedByCurrentUser: false,
        showBanner: false,
        showOverlay: true
    });

    const createComponent = createComponentFactory({
        component: DotUveLockOverlayComponent,
        imports: [DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'uve.editor.overlay.lock.unlocked.page.title': 'Unlock Title',
                    'uve.editor.overlay.lock.unlocked.page.description': 'Unlock Description',
                    'uve.editor.overlay.lock.locked.page.title': 'Lock Title',
                    'uve.editor.overlay.lock.locked.page.description': 'Lock Description'
                })
            },
            {
                provide: UVEStore,
                useValue: {
                    $toggleLockOptions: mockToggleLockOptions
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('when page is unlocked', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: true
            });
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should display the lock overlay', () => {
            const overlay = spectator.query(byTestId('lock-overlay'));
            expect(overlay).toBeTruthy();
        });

        it('should display the lock-open icon', () => {
            const icon = spectator.query('.lock-overlay__icon i.pi-lock-open');
            expect(icon).toBeTruthy();
        });

        it('should display the unlocked page title', () => {
            const title = spectator.query('.lock-overlay__title');
            expect(title).toBeTruthy();
            expect(title?.textContent?.trim()).toBe('Unlock Title');
        });

        it('should display the unlocked page message', () => {
            const message = spectator.query('.lock-overlay__message');
            expect(message).toBeTruthy();
            expect(message?.textContent?.trim()).toBe('Unlock Description');
        });

        it('should have proper test id', () => {
            const overlay = spectator.query(byTestId('lock-overlay'));
            expect(overlay).toBeTruthy();
        });
    });

    describe('when page is locked by another user', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'another-user',
                canLock: false,
                isLockedByCurrentUser: false,
                showBanner: true,
                showOverlay: true
            });
            spectator.detectChanges();
        });

        it('should display the lock icon', () => {
            const icon = spectator.query('.lock-overlay__icon i.pi-lock');
            expect(icon).toBeTruthy();
        });

        it('should display the locked page title', () => {
            const title = spectator.query('.lock-overlay__title');
            expect(title).toBeTruthy();
            expect(title?.textContent?.trim()).toBe('Lock Title');
        });

        it('should display the locked page message', () => {
            const message = spectator.query('.lock-overlay__message');
            expect(message).toBeTruthy();
            expect(message?.textContent?.trim()).toBe('Lock Description');
        });
    });

    describe('computed $overlayMessages', () => {
        it('should return unlock messages when page is not locked', () => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: true
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toEqual({
                icon: 'pi pi-lock-open',
                title: 'uve.editor.overlay.lock.unlocked.page.title',
                message: 'uve.editor.overlay.lock.unlocked.page.description'
            });
        });

        it('should return lock messages when page is locked by another user', () => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'another-user',
                canLock: false,
                isLockedByCurrentUser: false,
                showBanner: true,
                showOverlay: true
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toEqual({
                icon: 'pi pi-lock',
                title: 'uve.editor.overlay.lock.locked.page.title',
                message: 'uve.editor.overlay.lock.locked.page.description'
            });
        });

        it('should return null when page is locked by current user', () => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'current-user',
                canLock: true,
                isLockedByCurrentUser: true,
                showBanner: false,
                showOverlay: false
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toBeNull();
        });
    });
});
