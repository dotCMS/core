import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUveLockOverlayComponent } from './dot-uve-lock-overlay.component';

import { UVEStore } from '../../../store/dot-uve.store';
import { WorkflowLockOptions } from '../../../store/features/workflow/withWorkflow';

describe('DotUveLockOverlayComponent', () => {
    let spectator: Spectator<DotUveLockOverlayComponent>;

    const mockWorkflowLockOptions = signal<WorkflowLockOptions>({
        inode: 'test-inode',
        isLocked: false,
        lockedBy: '',
        canLock: true,
        isLockedByCurrentUser: false
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
                    $lockOptions: mockWorkflowLockOptions
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
            mockWorkflowLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false
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
            const iconWrapper = spectator.query(byTestId('lock-overlay-icon'));
            expect(iconWrapper).toBeTruthy();
            expect(iconWrapper?.querySelector('i.pi-lock-open')).toBeTruthy();
        });

        it('should display the unlocked page title', () => {
            const title = spectator.query(byTestId('lock-overlay-title'));
            expect(title).toBeTruthy();
            expect(title?.textContent?.trim()).toBe('Unlock Title');
        });

        it('should display the unlocked page message', () => {
            const message = spectator.query(byTestId('lock-overlay-message'));
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
            mockWorkflowLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'another-user',
                canLock: false,
                isLockedByCurrentUser: false
            });
            spectator.detectChanges();
        });

        it('should display the lock icon', () => {
            const iconWrapper = spectator.query(byTestId('lock-overlay-icon'));
            expect(iconWrapper).toBeTruthy();
            expect(iconWrapper?.querySelector('i.pi-lock')).toBeTruthy();
        });

        it('should display the locked page title', () => {
            const title = spectator.query(byTestId('lock-overlay-title'));
            expect(title).toBeTruthy();
            expect(title?.textContent?.trim()).toBe('Lock Title');
        });

        it('should display the locked page message', () => {
            const message = spectator.query(byTestId('lock-overlay-message'));
            expect(message).toBeTruthy();
            expect(message?.textContent?.trim()).toBe('Lock Description');
        });
    });

    describe('computed $overlayMessages', () => {
        it('should return unlock messages when page is not locked', () => {
            mockWorkflowLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toEqual({
                icon: 'pi pi-lock-open text-[1.75rem]!',
                title: 'uve.editor.overlay.lock.unlocked.page.title',
                message: 'uve.editor.overlay.lock.unlocked.page.description'
            });
        });

        it('should return lock messages when page is locked by another user', () => {
            mockWorkflowLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'another-user',
                canLock: false,
                isLockedByCurrentUser: false
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toEqual({
                icon: 'pi pi-lock text-[1.75rem]',
                title: 'uve.editor.overlay.lock.locked.page.title',
                message: 'uve.editor.overlay.lock.locked.page.description'
            });
        });

        it('should return lock messages when page is locked by current user', () => {
            mockWorkflowLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'current-user',
                canLock: true,
                isLockedByCurrentUser: true
            });
            spectator.detectChanges();

            const messages = spectator.component.$overlayMessages();
            expect(messages).toEqual({
                icon: 'pi pi-lock',
                title: 'uve.editor.overlay.lock.locked.page.title',
                message: 'uve.editor.overlay.lock.locked.page.description'
            });
        });
    });
});
