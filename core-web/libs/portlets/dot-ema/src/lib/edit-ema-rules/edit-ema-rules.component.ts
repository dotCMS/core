import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-edit-ema-rules',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './edit-ema-rules.component.html',
    styleUrls: ['./edit-ema-rules.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaRulesComponent {}
