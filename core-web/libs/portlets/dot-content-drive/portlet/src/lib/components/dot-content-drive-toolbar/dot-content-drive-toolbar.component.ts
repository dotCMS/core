import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotContentDriveBaseTypeSelectorComponent } from './components/dot-content-drive-base-type-selector/dot-content-drive-base-type-selector.component';
import { DotContentDriveContentTypeFieldComponent } from './components/dot-content-drive-content-type-field/dot-content-drive-content-type-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [
        ToolbarModule,
        ButtonModule,
        DotContentDriveTreeTogglerComponent,
        DotContentDriveBaseTypeSelectorComponent,
        DotContentDriveContentTypeFieldComponent,
        DotContentDriveSearchInputComponent
    ],
    providers: [],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveToolbarComponent {}
