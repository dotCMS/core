import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { DotAppsImportExportDialogComponent } from '../dot-apps-import-export-dialog/dot-apps-import-export-dialog.component';

@Component({
    selector: 'dot-apps-shell',
    templateUrl: './dot-apps-shell.component.html',
    styleUrl: './dot-apps-shell.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [RouterOutlet, DotAppsImportExportDialogComponent]
})
export class DotAppsShellComponent {}
