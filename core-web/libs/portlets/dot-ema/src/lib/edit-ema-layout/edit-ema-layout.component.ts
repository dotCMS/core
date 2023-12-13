import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-edit-ema-layout',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './edit-ema-layout.component.html',
    styleUrls: ['./edit-ema-layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaLayoutComponent {}
