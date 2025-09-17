import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotContentDriveBaseTypeSelectorComponent } from './components/dot-content-drive-base-type-selector/dot-content-drive-base-type-selector.component';
import { DotContentDriveContentTypeFieldComponent } from './components/dot-content-drive-content-type-field/dot-content-drive-content-type-field.component';
import { DotContentDriveLanguageFieldComponent } from './components/dot-content-drive-language-field/dot-content-drive-language-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [
        ToolbarModule,
        ButtonModule,
        DotContentDriveTreeTogglerComponent,
        DotContentDriveBaseTypeSelectorComponent,
        DotContentDriveContentTypeFieldComponent,
        DotContentDriveSearchInputComponent,
        DotContentDriveLanguageFieldComponent
    ],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);

    readonly $treeExpanded = this.#store.isTreeExpanded;
}
