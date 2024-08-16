import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'dot-edit-content',
    standalone: true,
    imports: [RouterModule],
    template: '<router-outlet />',
    styleUrls: ['./edit-content.shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentShellComponent {}
