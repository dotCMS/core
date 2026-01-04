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

    describe('computed $lockLoading', () => {
        it('should return true when loading', () => {
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

            expect(spectator.component.$lockLoading()).toBe(true);
        });

        it('should return false when not loading', () => {
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

            expect(spectator.component.$lockLoading()).toBe(false);
        });
    });

    describe('computed $unlockButton', () => {
        it('should return unlock button configuration', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'unlock-button-inode',
                isLocked: true,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: true,
                disabled: true,
                message: 'editpage.locked-by',
                args: ['John Doe']
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            const unlockButton = spectator.component.$unlockButton();
            expect(unlockButton).toEqual({
                show: true,
                inode: 'unlock-button-inode',
                disabled: true,
                loading: true,
                info: {
                    message: 'editpage.locked-by',
                    args: ['John Doe']
                }
            });
        });

        it('should reflect changes when toggleLockOptions changes', () => {
            const initialOptions: ToggleLockOptions = {
                inode: 'initial-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: true,
                loading: false,
                disabled: false,
                message: 'initial-message',
                args: []
            };
            spectator.setInput('toggleLockOptions', initialOptions);
            spectator.detectChanges();

            const initial = spectator.component.$unlockButton();
            expect(initial.inode).toBe('initial-inode');
            expect(initial.loading).toBe(false);

            const updatedOptions: ToggleLockOptions = {
                inode: 'updated-inode',
                isLocked: true,
                isLockedByCurrentUser: true,
                canLock: true,
                loading: true,
                disabled: false,
                message: 'updated-message',
                args: ['Updated User']
            };
            spectator.setInput('toggleLockOptions', updatedOptions);
            spectator.detectChanges();

            const updated = spectator.component.$unlockButton();
            expect(updated.inode).toBe('updated-inode');
            expect(updated.loading).toBe(true);
            expect(updated.info.message).toBe('updated-message');
            expect(updated.info.args).toEqual(['Updated User']);
        });
    });

    describe('computed $toggleLockOptions', () => {
        it('should be an alias for toggleLockOptions input', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'alias-test-inode',
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

            expect(spectator.component.$toggleLockOptions()).toEqual(lockOptions);
        });
    });

    describe('tooltip behavior', () => {
        it('should display tooltip when button is disabled and canLock is false', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: false,
                disabled: true,
                message: 'uve.editor.toggle.lock.button.disabled',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            const button = spectator.query(byTestId('toggle-lock-button'));
            // Verify button has disabled CSS class (which triggers tooltip display)
            expect(button).toHaveClass('lock-button--disabled');
            // Button should not be disabled by [disabled] attribute (only by loading state)
            expect(button).not.toBeDisabled();
        });

        it('should not display tooltip when canLock is true', () => {
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

            const button = spectator.query(byTestId('toggle-lock-button'));
            // When canLock is true, button should not have disabled CSS class
            expect(button).not.toHaveClass('lock-button--disabled');
        });
    });

    describe('CSS classes', () => {
        it('should have disabled class when canLock is false', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: false,
                disabled: true,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toHaveClass('lock-button--disabled');
        });

        it('should not have disabled class when canLock is true', () => {
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

            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).not.toHaveClass('lock-button--disabled');
        });
    });

    describe('edge cases', () => {
        it('should handle toggleLock when canLock is false and not emit event', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'test-inode',
                isLocked: false,
                isLockedByCurrentUser: false,
                canLock: false,
                loading: false,
                disabled: true,
                message: '',
                args: []
            };
            spectator.setInput('toggleLockOptions', lockOptions);
            spectator.detectChanges();

            // Call toggleLock directly
            spectator.component.toggleLock();

            // Should not emit any event because canLock is false
            expect(emittedEvents).toHaveLength(0);
        });

        it('should handle unlockPage with different inode than current', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'current-inode',
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

            // Call unlockPage with a different inode
            spectator.component.unlockPage('different-inode');

            expect(emittedEvents).toHaveLength(1);
            expect(emittedEvents[0]).toEqual({
                inode: 'different-inode',
                isLocked: true,
                isLockedByCurrentUser: false
            });
        });

        it('should handle rapid toggleLock calls', () => {
            const lockOptions: ToggleLockOptions = {
                inode: 'rapid-test-inode',
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

            // Call toggleLock multiple times rapidly
            spectator.component.toggleLock();
            spectator.component.toggleLock();
            spectator.component.toggleLock();

            // All events should be emitted
            expect(emittedEvents).toHaveLength(3);
            expect(emittedEvents[0].inode).toBe('rapid-test-inode');
            expect(emittedEvents[1].inode).toBe('rapid-test-inode');
            expect(emittedEvents[2].inode).toBe('rapid-test-inode');
        });
    });
});
