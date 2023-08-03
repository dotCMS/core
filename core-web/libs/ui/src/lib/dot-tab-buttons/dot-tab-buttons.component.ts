import { CommonModule, NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';

import { DotPageMode } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-tab-buttons',
    standalone: true,
    imports: [CommonModule, SplitButtonModule, ButtonModule, NgIf, NgClass],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTabButtonsComponent {
    @Output() eventOpen = new EventEmitter();
    @Output() changeState = new EventEmitter();
    @Input() mode: DotPageMode;
    @Input() options: SelectItem[];
    pageMode = DotPageMode;
    up = 'pi pi-angle-up';
    down = 'pi pi-angle-down';
    toggle = false;
    icon = this.down;

    onChangeState(event) {
        if (!event.target.value) {
            this.eventOpen.emit(event);
            this.toggle = !this.toggle;
            this.icon = this.toggle ? this.up : this.down;
        } else {
            this.changeState.emit(event);
        }
    }
}
