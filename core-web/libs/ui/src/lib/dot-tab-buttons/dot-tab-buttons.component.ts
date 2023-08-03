import { CommonModule, NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotPageMode } from '@dotcms/dotcms-models';

/**
 * This component is responsible to display the tab buttons for the edit page.
 *
 * @export
 * @class DotTabButtonsComponent
 */
@Component({
    selector: 'dot-tab-buttons',
    standalone: true,
    imports: [CommonModule, ButtonModule, NgIf, NgClass],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTabButtonsComponent {
    @Output() openMenu = new EventEmitter();
    @Output() clickOption = new EventEmitter();
    @Input() mode: DotPageMode;
    @Input() options: SelectItem[];
    pageMode = DotPageMode;
    dropDownOpenIcon = 'pi pi-angle-up';
    dropDownCloseIcon = 'pi pi-angle-down';
    toggle = false;
    icon = this.dropDownCloseIcon;
    OPEN_MENU = 'openMenu';

    onClickOption(event) {
        if (event.target.value === this.OPEN_MENU) {
            this.showMenu(event);
        } else {
            this.clickOption.emit(event);
        }
    }

    showMenu(event) {
        this.toggle = !this.toggle;
        this.toggleIcon();
        this.openMenu.emit(event);
    }

    toggleIcon() {
        this.icon = this.toggle ? this.dropDownOpenIcon : this.dropDownCloseIcon;
    }
}
