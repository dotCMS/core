import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output
} from '@angular/core';

import { NavigationBarItem } from '../../shared/models';

@Component({
    selector: 'dot-edit-ema-navigation-bar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './edit-ema-navigation-bar.component.html',
    styleUrls: ['./edit-ema-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaNavigationBarComponent implements OnInit {
    @Input() items: NavigationBarItem[];
    @Input() selectedKey: string;
    @Output() action: EventEmitter<NavigationBarItem> = new EventEmitter();

    selected: string;

    ngOnInit(): void {
        this.selected = this.selectedKey;
    }

    onClick(item: NavigationBarItem) {
        this.selected = item.key;
        this.action.emit(item);
    }
}
