import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-tree-toggler',
    templateUrl: './dot-content-drive-search-input.component.html',
    styleUrl: './dot-content-drive-search-input.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveSearchInputComponent {
    #store = inject(DotContentDriveStore);
}
