import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

@Component({
    selector: 'dot-edit-ema-toolbar',
    standalone: true,
    imports: [CommonModule, MenuModule, ButtonModule],
    templateUrl: './edit-ema-toolbar.component.html',
    styleUrls: ['./edit-ema-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaToolbarComponent {
    @Input() pageTitle: string;
}
