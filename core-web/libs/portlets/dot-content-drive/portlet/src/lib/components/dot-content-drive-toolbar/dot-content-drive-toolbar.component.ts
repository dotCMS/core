import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [ToolbarModule, ButtonModule],
    providers: [],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveToolbarComponent {}
