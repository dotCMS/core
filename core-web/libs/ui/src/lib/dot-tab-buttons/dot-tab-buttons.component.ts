import { NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';

import { DotPageMode } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-tab-buttons',
    standalone: true,
    imports: [SplitButtonModule, ButtonModule, NgIf, NgClass],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTabButtonsComponent {
    @Output() eventOpen = new EventEmitter();
    @Output() changeState = new EventEmitter();
    @Input() mode: DotPageMode;
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
