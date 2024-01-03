import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dot-edit-ema-access-denied',
    standalone: true,
    imports: [ButtonModule, RouterLink],
    templateUrl: './edit-ema-access-denied.component.html',
    styleUrls: ['./edit-ema-access-denied.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaAccessDeniedComponent {}
