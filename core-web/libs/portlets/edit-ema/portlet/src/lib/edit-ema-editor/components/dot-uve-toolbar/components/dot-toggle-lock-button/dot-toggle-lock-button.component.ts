import { Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

export interface ToggleLockOptions {
    inode: string;
    isLocked: boolean;
    isLockedByCurrentUser: boolean;
    canLock: boolean;
    loading: boolean;
    disabled: boolean;
    message: string;
    args: string[];
}

export interface ToggleLockEvent {
    inode: string;
    isLocked: boolean;
    isLockedByCurrentUser: boolean;
}

@Component({
    selector: 'dot-toggle-lock-button',
    templateUrl: './dot-toggle-lock-button.component.html',
    styleUrls: ['./dot-toggle-lock-button.component.scss'],
    imports: [ButtonModule, TooltipModule, DotMessagePipe]
})
export class DotToggleLockButtonComponent {
    // Inputs - data down from container
    toggleLockOptions = input.required<ToggleLockOptions>();

    // Outputs - events up to container
    toggleLockClick = output<ToggleLockEvent>();

    // Local computed - button label based on lock state
    $buttonLabel = computed(() => {
        const isLocked = this.toggleLockOptions()?.isLocked;
        return isLocked
            ? 'uve.editor.toggle.lock.button.locked'
            : 'uve.editor.toggle.lock.button.unlocked';
    });

    // Legacy computed for template compatibility
    $toggleLockOptions = this.toggleLockOptions;
    $lockLoading = computed(() => this.toggleLockOptions().loading);
    $unlockButton = computed(() => ({
        show: true,
        inode: this.toggleLockOptions().inode,
        disabled: this.toggleLockOptions().disabled,
        loading: this.toggleLockOptions().loading,
        info: {
            message: this.toggleLockOptions().message,
            args: this.toggleLockOptions().args
        }
    }));

    /**
     * Toggles the lock state of the current page.
     * Emits event to parent container to handle the actual toggle.
     */
    toggleLock() {
        const { inode, isLocked, isLockedByCurrentUser, canLock } = this.toggleLockOptions();

        if (!canLock) {
            return;
        }

        // Emit event instead of directly calling store
        this.toggleLockClick.emit({
            inode,
            isLocked,
            isLockedByCurrentUser
        });
    }

    /**
     * Unlocks a page with the specified inode (legacy method for backward compatibility).
     *
     * @param {string} inode
     * @memberof DotToggleLockButtonComponent
     */
    unlockPage(inode: string) {
        // Emit event instead of directly calling store
        this.toggleLockClick.emit({
            inode,
            isLocked: true,
            isLockedByCurrentUser: false
        });
    }
}
