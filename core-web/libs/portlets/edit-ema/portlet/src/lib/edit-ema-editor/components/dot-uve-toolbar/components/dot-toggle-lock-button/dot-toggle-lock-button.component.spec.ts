import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToggleLockButtonComponent } from './dot-toggle-lock-button.component';

import { ToggleLockOptions, UnlockOptions } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';

describe('DotToggleLockButtonComponent', () => {
    let spectator: Spectator<DotToggleLockButtonComponent>;
    let store: Partial<InstanceType<typeof UVEStore>>;

    const mockToggleLockOptions = signal<ToggleLockOptions | null>({
        inode: 'test-inode',
        isLocked: false,
        lockedBy: '',
        canLock: true,
        isLockedByCurrentUser: false,
        showBanner: false,
        showOverlay: false
    });

    const mockUnlockButton = signal<UnlockOptions | null>(null);
    const mockLockLoading = signal<boolean>(false);
    const mockToggleLock = jest.fn();

    const createComponent = createComponentFactory({
        component: DotToggleLockButtonComponent,
        imports: [ButtonModule, TooltipModule, DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'uve.editor.toggle.lock.button.unlocked': 'Unlock Page',
                    'uve.editor.toggle.lock.button.locked': 'Lock Page'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        store = {
            $toggleLockOptions: mockToggleLockOptions,
            $unlockButton: mockUnlockButton,
            lockLoading: mockLockLoading,
            toggleLock: mockToggleLock
        };

        spectator = createComponent({
            providers: [
                {
                    provide: UVEStore,
                    useValue: store
                }
            ]
        });

        jest.clearAllMocks();
    });

    describe('when feature flag is enabled (new toggle button)', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: false
            });
            mockUnlockButton.set(null);
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should display toggle lock button', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toBeTruthy();
        });

        it('should display lock-open icon when page is unlocked', () => {
            const button = spectator.query(byTestId('toggle-lock-button')) as HTMLElement;
            expect(button?.querySelector('.pi-lock-open')).toBeTruthy();
        });

        it('should display unlocked label when page is unlocked', () => {
            const label = spectator.component.$buttonLabel();
            expect(label).toBe('uve.editor.toggle.lock.button.unlocked');
        });

        // Styling is handled by PrimeNG defaults; we only override locked state colors in template.

        it('should call store.toggleLock with correct params when clicked and unlocked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(mockToggleLock).toHaveBeenCalledWith('test-inode', false, false);
        });
    });

    describe('when page is locked by current user', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode-locked',
                isLocked: true,
                lockedBy: 'current-user',
                canLock: true,
                isLockedByCurrentUser: true,
                showBanner: false,
                showOverlay: false
            });
            mockUnlockButton.set(null);
            spectator.detectChanges();
        });

        it('should display lock icon when page is locked', () => {
            const button = spectator.query(byTestId('toggle-lock-button')) as HTMLElement;
            expect(button?.querySelector('.pi-lock')).toBeTruthy();
        });

        it('should display locked label when page is locked', () => {
            const label = spectator.component.$buttonLabel();
            expect(label).toBe('uve.editor.toggle.lock.button.locked');
        });

        // Styling is handled by PrimeNG defaults; we only override locked state colors in template.

        it('should call store.toggleLock with correct params when clicked and locked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(mockToggleLock).toHaveBeenCalledWith('test-inode-locked', true, true);
        });
    });

    describe('when page is locked by another user', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode-locked-other',
                isLocked: true,
                lockedBy: 'another-user',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: true,
                showOverlay: true
            });
            mockUnlockButton.set(null);
            spectator.detectChanges();
        });

        it('should display lock icon', () => {
            const button = spectator.query(byTestId('toggle-lock-button')) as HTMLElement;
            expect(button?.querySelector('.pi-lock')).toBeTruthy();
        });

        it('should call store.toggleLock with isLockedByCurrentUser=false when clicked', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            expect(mockToggleLock).toHaveBeenCalledWith('test-inode-locked-other', true, false);
        });
    });

    describe('when loading', () => {
        beforeEach(() => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: false
            });
            mockLockLoading.set(true);
            mockUnlockButton.set(null);
            spectator.detectChanges();
        });

        it('should disable button when loading', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toBeDisabled();
        });

        it('should not call toggleLock when button is clicked during loading', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            spectator.click(button);

            // Button is disabled, so click won't trigger the handler
            expect(mockToggleLock).not.toHaveBeenCalled();
        });
    });

    describe('when feature flag is disabled (legacy unlock button)', () => {
        beforeEach(() => {
            mockToggleLockOptions.set(null);
            mockUnlockButton.set({
                inode: 'legacy-inode',
                disabled: false,
                loading: false,
                info: {
                    message: 'Page locked by {0}',
                    args: ['Another User']
                }
            });
            spectator.detectChanges();
        });

        it('should display legacy unlock button', () => {
            const button = spectator.query(byTestId('uve-toolbar-unlock-button'));
            expect(button).toBeTruthy();
        });

        it('should not display new toggle button', () => {
            const button = spectator.query(byTestId('toggle-lock-button'));
            expect(button).toBeFalsy();
        });

        it('should call unlockPage method when legacy button is clicked', () => {
            const button = spectator.query(byTestId('uve-toolbar-unlock-button'));
            spectator.click(button);

            expect(mockToggleLock).toHaveBeenCalledWith('legacy-inode', true, false);
        });
    });

    describe('computed $buttonLabel', () => {
        it('should return unlocked label when isLocked is false', () => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: false
            });
            spectator.detectChanges();

            expect(spectator.component.$buttonLabel()).toBe(
                'uve.editor.toggle.lock.button.unlocked'
            );
        });

        it('should return locked label when isLocked is true', () => {
            mockToggleLockOptions.set({
                inode: 'test-inode',
                isLocked: true,
                lockedBy: 'user',
                canLock: true,
                isLockedByCurrentUser: true,
                showBanner: false,
                showOverlay: false
            });
            spectator.detectChanges();

            expect(spectator.component.$buttonLabel()).toBe('uve.editor.toggle.lock.button.locked');
        });
    });

    describe('toggleLock method', () => {
        it('should extract correct parameters from $toggleLockOptions and call store', () => {
            mockToggleLockOptions.set({
                inode: 'method-test-inode',
                isLocked: false,
                lockedBy: '',
                canLock: true,
                isLockedByCurrentUser: false,
                showBanner: false,
                showOverlay: false
            });
            spectator.detectChanges();

            spectator.component.toggleLock();

            expect(mockToggleLock).toHaveBeenCalledWith('method-test-inode', false, false);
        });

        it('should handle locked state in method call', () => {
            mockToggleLockOptions.set({
                inode: 'locked-method-inode',
                isLocked: true,
                lockedBy: 'current',
                canLock: true,
                isLockedByCurrentUser: true,
                showBanner: false,
                showOverlay: false
            });
            spectator.detectChanges();

            spectator.component.toggleLock();

            expect(mockToggleLock).toHaveBeenCalledWith('locked-method-inode', true, true);
        });
    });

    describe('unlockPage method (legacy)', () => {
        it('should call store.toggleLock with unlock parameters', () => {
            spectator.component.unlockPage('legacy-unlock-inode');

            expect(mockToggleLock).toHaveBeenCalledWith('legacy-unlock-inode', true, false);
        });
    });
});
