import { CommonModule, NgClass, NgIf } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output
} from '@angular/core';

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

type CustomEventTarget = EventTarget & {
    value: string;
};

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
export class DotTabButtonsComponent implements OnInit {
    @Output() openMenu = new EventEmitter<{ event: PointerEvent; id: string }>();
    @Output() clickOption = new EventEmitter();
    @Input() activeId: string;
    @Input() options: SelectItem<TabButtonOptions>[];

    _options: SelectItem<TabButtonOptions>[] = [];

    protected readonly dropDownOpenIcon = 'pi pi-angle-up';
    protected readonly dropDownCloseIcon = 'pi pi-angle-down';
    readonly OPEN_MENU = '-openMenu';

    ngOnInit() {
        // We don't want reference issues with the options, so we clone them.
        this._options = structuredClone(this.options).map((option) => {
            if (option.value.showDropdownButton) {
                option.value.toggle = false;
                option.value.icon = this.dropDownCloseIcon;
            }

            return option;
        });
    }

    /**
     * Handles the click event on the tab buttons.
     * @param event
     */
    onClickOption(event: PointerEvent) {
        const { value } = event.target as CustomEventTarget;

        if (
            this._options.find(({ value: { id } }) => value.includes(id + this.OPEN_MENU))?.value
                .showDropdownButton
        ) {
            this.showMenu(event);
        } else {
            this.clickOption.emit(event);
        }
    }

    /**
     * Handles the click event on the menu button.
     * @param event
     */
    showMenu(event: PointerEvent) {
        event.stopPropagation();

        const { value } = event.target as CustomEventTarget;

        this._options = this._options.map((option) => {
            if (value.includes(option.value.id)) {
                option.value.toggle = !option.value.toggle;
                option.value.icon = option.value.toggle
                    ? this.dropDownOpenIcon
                    : this.dropDownCloseIcon;
            }

            return option;
        });

        this.openMenu.emit({ event, id: value.replace(this.OPEN_MENU, '') });
    }

    /**
     * Handles the click event on the document to reset the dropdowns state.
     *
     * @memberof DotTabButtonsComponent
     */
    @HostListener('document:click', ['$event'])
    resetDropdowns() {
        this._options = this._options.map((option) => {
            option.value.toggle = false;
            option.value.icon = this.dropDownCloseIcon;

            return option;
        });
    }
}
