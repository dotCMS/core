import { Component, computed, inject } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-lock-overlay',
    imports: [DotMessagePipe],
    templateUrl: './dot-uve-lock-overlay.component.html',
    styleUrls: ['./dot-uve-lock-overlay.component.scss']
})
export class DotUveLockOverlayComponent {
    readonly #store = inject(UVEStore);

    $toggleLockOptions = this.#store.$toggleLockOptions;

    $overlayMessages = computed(() => {
        const { isLocked, isLockedByCurrentUser } = this.$toggleLockOptions();

        if (!isLocked) {
            return {
                icon: 'pi pi-lock-open',
                title: 'uve.editor.overlay.lock.unlocked.page.title',
                message: 'uve.editor.overlay.lock.unlocked.page.description'
            };
        }

        if (!isLockedByCurrentUser) {
            return {
                title: 'uve.editor.overlay.lock.locked.page.title',
                icon: 'pi pi-lock',
                message: 'uve.editor.overlay.lock.locked.page.description'
            };
        }

        return null;
    });
}
