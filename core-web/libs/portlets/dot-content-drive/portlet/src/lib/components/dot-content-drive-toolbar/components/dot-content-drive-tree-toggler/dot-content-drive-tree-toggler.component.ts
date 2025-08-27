import { ChangeDetectionStrategy, Component, HostListener, inject } from '@angular/core';

import { ButtonDirective } from 'primeng/button';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-tree-toggler',
    templateUrl: './dot-content-drive-tree-toggler.component.html',
    styleUrl: './dot-content-drive-tree-toggler.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    hostDirectives: [ButtonDirective],
    host: {
        class: 'p-button-icon-only p-button-rounded p-button-text p-button-sm'
    }
})
export class DotContentDriveTreeTogglerComponent {
    #store = inject(DotContentDriveStore);

    @HostListener('click')
    toggleTree(): void {
        this.#store.setIsTreeExpanded(!this.#store.isTreeExpanded());
    }
}
