import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'dot-edit-content',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './edit-content.shell.component.html',
    styleUrls: ['./edit-content.shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditContentShellComponent {}
