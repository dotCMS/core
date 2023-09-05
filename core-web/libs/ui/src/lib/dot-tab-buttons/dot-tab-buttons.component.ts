import { CommonModule, NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageMode } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * This component is responsible to display the tab buttons for the edit page.
 *
 * @export
 * @class DotTabButtonsComponent
 */
@Component({
    selector: 'dot-tab-buttons',
    standalone: true,
    imports: [CommonModule, ButtonModule, NgIf, NgClass, TooltipModule, DotMessagePipe],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTabButtonsComponent {
    @Output() openMenu = new EventEmitter();
    @Output() clickOption = new EventEmitter();
    @Input() mode: DotPageMode;
    @Input() options: SelectItem[];
    protected readonly pageMode = DotPageMode;
    protected readonly dropDownOpenIcon = 'pi pi-angle-up';
    protected readonly dropDownCloseIcon = 'pi pi-angle-down';
    readonly OPEN_MENU = 'openMenu';
    toggle = false;
    icon = this.dropDownCloseIcon;

    /**
     * Handles the click event on the tab buttons.
     * @param event
     */
    onClickOption(event) {
        if (event.target.value === this.OPEN_MENU) {
            this.showMenu(event);
        } else if (event.target.value !== this.mode) {
            this.clickOption.emit(event);
        }
    }

    /**
     * Handles the click event on the menu button.
     * @param event
     */
    showMenu(event) {
        this.toggle = !this.toggle;
        this.toggleIcon();
        this.openMenu.emit(event);
    }

    toggleIcon() {
        this.icon = this.toggle ? this.dropDownOpenIcon : this.dropDownCloseIcon;
    }
}
