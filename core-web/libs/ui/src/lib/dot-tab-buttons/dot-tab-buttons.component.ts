import { NgClass, NgFor, NgIf } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

interface TabButtonOptions {
    id: string;
    toggle?: boolean;
    icon?: string;
    showDropdownButton: boolean;
}

/**
 * This component is responsible to display the tab buttons for the edit page.
 *
 * @export
 * @class DotTabButtonsComponent
 */
@Component({
    selector: 'dot-tab-buttons',
    standalone: true,
    imports: [NgFor, ButtonModule, NgIf, NgClass, TooltipModule, DotMessagePipe],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss']
})
export class DotTabButtonsComponent implements OnChanges {
    @Output() openMenu = new EventEmitter<{ event: PointerEvent; menuId: string }>();
    @Output() clickOption = new EventEmitter<{ event: PointerEvent; optionId: string }>();
    @Input() activeId: string;
    @Input() options: SelectItem<TabButtonOptions>[];
    @Output() dropdownClick = new EventEmitter();

    _options: SelectItem<TabButtonOptions>[] = [];

    protected readonly dropDownOpenIcon = 'pi pi-angle-up';
    protected readonly dropDownCloseIcon = 'pi pi-angle-down';

    ngOnChanges(changes: SimpleChanges) {
        if (changes.options) {
            // We don't want reference issues with the options, so we clone them.
            this._options = structuredClone(this.options).map((option) => {
                if (option.value.showDropdownButton) {
                    option.value.toggle = false;
                }

                return option;
            });
        }
    }

    /**
     * Handles the click event on the tab buttons.
     * @param event
     */
    onClickOption(event: PointerEvent, optionId: string) {
        if (optionId === this.activeId) {
            return;
        }

        this.clickOption.emit({
            event,
            optionId
        });
    }

    /**
     * Handles the click event on the menu button.
     * @param event
     */
    onClickDropdown(event: PointerEvent, menuId: string) {
        event.stopPropagation();

        this._options = this._options.map((option) => {
            if (menuId.includes(option.value.id)) {
                option.value.toggle = !option.value.toggle;
            }

            return option;
        });

        this.openMenu.emit({ event, menuId });
    }

    /**
     * Resets all the dropdowns to closed state.
     *
     * @memberof DotTabButtonsComponent
     */
    resetDropdowns() {
        this._options = this._options.map((option) => {
            option.value.toggle = false;

            return option;
        });
    }

    resetDropdownById(id: string) {
        this._options = this._options.map((option) => {
            if (option.value.id === id) option.value.toggle = false;

            return option;
        });
    }
}
