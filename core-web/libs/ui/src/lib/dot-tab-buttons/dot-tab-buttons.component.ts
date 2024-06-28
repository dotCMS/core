import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '../dot-message/dot-message.pipe';

interface TabButtonOptions {
    id: string;
    toggle?: boolean;
    icon?: string;
    showDropdownButton: boolean;
    shouldRefresh?: boolean;
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
    imports: [ButtonModule, NgClass, TooltipModule, DotMessagePipe],
    templateUrl: './dot-tab-buttons.component.html',
    styleUrls: ['./dot-tab-buttons.component.scss']
})
export class DotTabButtonsComponent implements OnChanges {
    @Output() openMenu = new EventEmitter<{
        event: PointerEvent;
        menuId: string;
        target?: HTMLElement;
    }>();
    @Output() clickOption = new EventEmitter<{ event: PointerEvent; optionId: string }>();
    @Input() activeId: string;
    @Input() options: SelectItem<TabButtonOptions>[];
    @Output() dropdownClick = new EventEmitter();

    _options: SelectItem<TabButtonOptions>[] = [];

    protected readonly dropDownOpenIcon = 'pi pi-angle-up';
    protected readonly dropDownCloseIcon = 'pi pi-angle-down';

    ngOnChanges(changes: SimpleChanges) {
        if (changes.options) {
            this._options = this.options.map((option) => ({
                ...option,
                value: {
                    ...option.value,
                    toggle: option.value.showDropdownButton ? false : undefined
                }
            })); // We don't want reference issues with the options, so we create a new object.
        }
    }

    /**
     * Handles the click event on the tab buttons.
     * @param event
     */
    onClickOption(event: PointerEvent, optionId: string) {
        if (optionId === this.activeId && !this.shouldRefresh(optionId)) {
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
        // This method is public so you can easily break everything if you force this to open.
        if (!this.shouldOpenMenu(menuId)) return;

        this._options = this._options.map((option) => {
            if (menuId.includes(option.value.id)) {
                option.value.toggle = !option.value.toggle;
            }

            return option;
        });

        const target = event?.target as HTMLElement;
        const menuOption = target?.closest('.dot-tab') as HTMLElement;

        this.openMenu.emit({ event, menuId, target: menuOption });
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

    /**
     * Resets the dropdown with the given id to closed state.
     *
     * @param {string} id
     * @memberof DotTabButtonsComponent
     */
    resetDropdownById(id: string) {
        this._options = this._options.map((option) => {
            if (option.value.id === id) option.value.toggle = false;

            return option;
        });
    }

    /**
     * Checks if the dropdown you clicked is showed in the dom
     *
     * @private
     * @param {string} menuId
     * @return {*}  {boolean}
     * @memberof DotTabButtonsComponent
     */
    private shouldOpenMenu(menuId: string): boolean {
        return Boolean(
            this._options.find(
                (option) => option.value.id === menuId && option.value.showDropdownButton
            )
        );
    }

    /**
     * Checks if the option you clicked should refresh the page
     *
     * @private
     * @param {string} menuId
     * @return {*}  {boolean}
     * @memberof DotTabButtonsComponent
     */
    private shouldRefresh(menuId: string): boolean {
        return Boolean(
            this._options.find((option) => option.value.id === menuId && option.value.shouldRefresh)
        );
    }
}
