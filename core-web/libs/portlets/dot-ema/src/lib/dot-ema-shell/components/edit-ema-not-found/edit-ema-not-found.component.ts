import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dot-edit-ema-not-found',
    standalone: true,
    imports: [ButtonModule, RouterLink],
    templateUrl: './edit-ema-not-found.component.html',
    styleUrls: ['./edit-ema-not-found.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaNotFoundComponent {}
