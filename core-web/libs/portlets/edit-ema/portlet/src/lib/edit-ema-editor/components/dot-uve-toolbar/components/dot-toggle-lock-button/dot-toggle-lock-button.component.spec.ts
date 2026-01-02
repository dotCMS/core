import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotToggleLockButtonComponent,
    ToggleLockEvent,
    ToggleLockOptions
} from './dot-toggle-lock-button.component';

describe('DotToggleLockButtonComponent - Presentational', () => {
    let spectator: Spectator<DotToggleLockButtonComponent>;
    let emittedEvents: ToggleLockEvent[] = [];

    const createComponent = createComponentFactory({
        component: DotToggleLockButtonComponent,
        imports: [ButtonModule, TooltipModule, DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'uve.editor.toggle.lock.button.unlocked': 'Unlock Page',
                    'uve.editor.toggle.lock.button.locked': 'Lock Page',
                    'editpage.toolbar.page.release.lock.locked.by.user':
                        'Page locked by {0}. Click to take over the lock.',
                    'editpage.locked-by': 'Page locked by {0}'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        emittedEvents = [];
        spectator = createComponent();

        // Subscribe to output events
        spectator.component.toggleLockClick.subscribe((event) => {
            emittedEvents.push(event);
        });
    });

    describe('Component Creation', () => {
        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });
    });

    describe('when page is unlocked', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: 'editpage.toolbar.page.release.lock.locked.by.user',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should display toggle lock button', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toBeTruthy();
        });

        it('should display lock-open icon when page is unlocked', () => {
            const icon = spectator.query('.lock-button i.pi-lock-open');
            expect(icon).toBeTruthy();
        });

        it('should display unlocked label', () => {
            const label = spectator.component.$buttonLabel();
            expect(label).toBe('uve.editor.toggle.lock.button.unlocked');
        });

        it('should have unlocked CSS class', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toHaveClass('lock-button--unlocked');
            expect(button).not.toHaveClass('lock-button--locked');
        });

        it('should emit toggleLockClick event with correct params when clicked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false
            });
        });
    });

    describe('when page is locked by current user', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode-locked',
                isLocked: true,
                isLockedByCurrentUser: true,
                canLock: true,
                loading: false,
                disabled: false,
                message: 'editpage.toolbar.page.release.lock.locked.by.user',
                args: ['Current User']
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should display lock icon', () => {
            const icon = spectator.query('.lock-button i.pi-lock');
            expect(icon).toBeTruthy();
        });

        it('should display locked label', () => {
            const label = spectator.component.$buttonLabel();
            expect(label).toBe('uve.editor.toggle.lock.button.locked');
        });

        it('should have locked CSS class', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toHaveClass('lock-button--locked');
            expect(button).not.toHaveClass('lock-button--unlocked');
        });

        it('should emit toggleLockClick event with correct params when clicked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'test-inode-locked',
                isLocked: true,
                isLockedByCurrentUser: true
            });
        });
    });

    describe('when page is locked by another user', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode-locked-other',
                isLocked: true,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: 'editpage.locked-by',
                args: ['Another User']
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should display lock icon', () => {
            const icon = spectator.query('.lock-button i.pi-lock');
            expect(icon).toBeTruthy();
        });

        it('should emit toggleLockClick event with isLockedByCurrentUser=false when clicked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'test-inode-locked-other',
                isLocked: true,
                isLockedByCurrentUser: false
            });
        });
    });

    describe('when loading', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: true,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should disable button when loading', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toBeDisabled();
        });

        it('should not emit event when button is clicked during loading', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            // Button is disabled, so click won't trigger the handler
            expect(emittedEvents).toHaveLength(0);
        });
    });

    describe('when canLock is false', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: false,
                disabled: true,
                message: 'editpage.locked-by',
                args: ['Some User']
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should not emit event when canLock is false', () => {
            spectator.component.toggleLock();
            expect(emittedEvents).toHaveLength(0);
        });
    });

    describe('computed $buttonLabel', () => {
        it('should return unlocked label when isLocked is false', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            expect(spectator.component.$buttonLabel()).toBe(
                'uve.editor.toggle.lock.button.unlocked'
            );
        });

        it('should return locked label when isLocked is true', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: true,
                isLockedByCurrentUser: true,
                canLock: true,
                loading: false,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            expect(spectator.component.$buttonLabel()).toBe('uve.editor.toggle.lock.button.locked');
        });
    });

    describe('toggleLock method', () => {
        it('should emit correct event parameters', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'method-test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            spectator.component.toggleLock();

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'method-test-inode',
                isLocked: false,
                isLockedByCurrentUser: false
            });
        });

        it('should handle locked state in event emission', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'locked-method-inode',
                isLocked: true,
                isLockedByCurrentUser: true,
                canLock: true,
                loading: false,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            spectator.component.toggleLock();

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'locked-method-inode',
                isLocked: true,
                isLockedByCurrentUser: true
            });
        });
    });

    describe('unlockPage method (legacy)', () => {
        it('should emit event with unlock parameters', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            spectator.component.unlockPage('legacy-unlock-inode');

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'legacy-unlock-inode',
                isLocked: true,
                isLockedByCurrentUser: false
            });
        });
    });

    describe('legacy unlock button support', () => {
        beforeEach(() => {
            const lockOptions: ToggleLockOptions = {
                inode: 'legacy-inode',
                isLocked: true,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: false,
                disabled: true,
                message: 'editpage.locked-by',
                args: ['Another User']
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();
        });

        it('should display unlock button for legacy mode', () => {
            const button = spectator.query(byTestId('uve-toolbar-unlock-button'));
            expect(button).toBeTruthy();
        });

        it('should emit event when legacy button is clicked', () => {
            const button = spectator.query(byTestId('uve-toolbar-unlock-button'));
            spectator.click(button);

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'legacy-inode',
                isLocked: true,
                isLockedByCurrentUser: false
            });
        });
    });
});
