import { Component, computed, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-toggle-lock-button',
    templateUrl: './dot-toggle-lock-button.component.html',
    imports: [ButtonModule, TooltipModule, DotMessagePipe]
})
export class DotToggleLockButtonComponent {
    readonly #store = inject(UVEStore);

    $unlockButton = this.#store.$unlockButton;
    $toggleLockOptions = this.#store.$toggleLockOptions;
    $lockLoading = this.#store.lockLoading;

    $buttonLabel = computed(() => {
        const isLocked = this.$toggleLockOptions()?.isLocked;
        return isLocked
            ? 'uve.editor.toggle.lock.button.locked'
            : 'uve.editor.toggle.lock.button.unlocked';
    });

    /**
     * Toggles the lock state of the current page.
     * If the page is unlocked, it will lock it for the current user.
     * If the page is locked by the current user, it will unlock it.
     * If the page is locked by another user, it will attempt to take over the lock.
     */
    toggleLock() {
        const { inode, isLocked, isLockedByCurrentUser, canLock } = this.$toggleLockOptions();

        if (!canLock) {
            return;
        }

        this.#store.toggleLock(inode, isLocked, isLockedByCurrentUser);
    }

    /**
     * Unlocks a page with the specified inode (legacy method for backward compatibility).
     *
     * @param {string} inode
     * @memberof DotToggleLockButtonComponent
     */
    unlockPage(inode: string) {
        this.#store.toggleLock(inode, true, false);
    }
}
