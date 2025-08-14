import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-search-input',
    templateUrl: './dot-content-drive-search-input.component.html',
    styleUrl: './dot-content-drive-search-input.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [IconFieldModule, InputIconModule, InputTextModule]
})
export class DotContentDriveSearchInputComponent {
    #store = inject(DotContentDriveStore);
}
