import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'dot-edit-content',
    standalone: true,
    imports: [RouterModule],
    templateUrl: './edit-content.shell.component.html',
    styleUrls: ['./edit-content.shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentShellComponent {}
