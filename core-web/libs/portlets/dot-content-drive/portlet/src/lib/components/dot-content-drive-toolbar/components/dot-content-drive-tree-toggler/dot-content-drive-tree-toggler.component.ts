import {
    ChangeDetectionStrategy,
    Component,
    HostBinding,
    HostListener,
    inject
} from '@angular/core';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-tree-toggler',
    templateUrl: './dot-content-drive-tree-toggler.component.html',
    styleUrl: './dot-content-drive-tree-toggler.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveTreeTogglerComponent {
    #store = inject(DotContentDriveStore);

    @HostListener('click')
    toggleTree(): void {
        this.#store.setIsTreeExpanded(!this.#store.isTreeExpanded());
    }

    @HostBinding('class.active')
    get isActive(): boolean {
        return this.#store.isTreeExpanded();
    }
}
