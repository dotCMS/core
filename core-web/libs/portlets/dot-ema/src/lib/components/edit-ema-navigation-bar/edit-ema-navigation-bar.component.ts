import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { NavigationBarItem } from '../../shared/models';

@Component({
    selector: 'dot-edit-ema-navigation-bar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './edit-ema-navigation-bar.component.html',
    styleUrls: ['./edit-ema-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaNavigationBarComponent {
    @Input() items: NavigationBarItem[];
    @Input() selectedKey: string;
    @Output() action: EventEmitter<NavigationBarItem> = new EventEmitter();

    onClick(item: NavigationBarItem) {
        this.action.emit(item);
    }
}
