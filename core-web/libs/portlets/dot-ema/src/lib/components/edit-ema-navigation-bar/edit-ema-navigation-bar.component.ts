import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';

import { NavigationBarItem } from '../../shared/models';

@Component({
    selector: 'dot-edit-ema-navigation-bar',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './edit-ema-navigation-bar.component.html',
    styleUrls: ['./edit-ema-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaNavigationBarComponent {
    @Input() items: NavigationBarItem[];
}
