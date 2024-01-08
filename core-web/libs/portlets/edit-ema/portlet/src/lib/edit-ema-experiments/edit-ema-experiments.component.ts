import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-edit-ema-experiments',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './edit-ema-experiments.component.html',
    styleUrls: ['./edit-ema-experiments.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaExperimentsComponent {}
