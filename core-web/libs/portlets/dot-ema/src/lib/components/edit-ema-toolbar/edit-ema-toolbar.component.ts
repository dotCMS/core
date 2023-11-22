import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

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
    label = 'Actions';

    items = [
        {
            label: 'Update',
            icon: 'pi pi-refresh',
            command: () => {
                this.label = 'Updated';
            }
        },
        {
            label: 'Delete',
            icon: 'pi pi-times',
            command: () => {
                this.label = 'Delete';
            }
        },
        {
            label: 'Angular',
            icon: 'pi pi-external-link',
            command: () => {
                this.label = 'Angular';
            }
        },
        {
            label: 'Router',
            icon: 'pi pi-upload',
            command: () => {
                this.label = 'Router';
            }
        }
    ];
}
