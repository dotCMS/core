import {
    Component,
    EventEmitter,
    HostBinding,
    HostListener,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

/**
 * The ActionButtonComponent is a configurable button with
 * options to add to the primary actions in the portlets.
 * @export
 * @class ActionButtonComponent
 */
@Component({
    selector: 'dot-action-button',
    styleUrls: ['./dot-action-button.component.scss'],
    templateUrl: 'dot-action-button.component.html',
    imports: [ButtonModule, MenuModule]
})
export class DotActionButtonComponent implements OnInit, OnChanges {
    @ViewChild('menu')
    menu: Menu;

    @Input()
    disabled: boolean;

    @Input()
    icon: string;

    @Input()
    label: string;

    @Input()
    model: MenuItem[];

    @Input()
    selected: boolean;

    @Output()
    press: EventEmitter<MouseEvent> = new EventEmitter();

    @HostBinding('class.action-button--no-label')
    isNotLabeled = true;

    @HostListener('click', ['$event'])
    public onClick(event: MouseEvent): void {
        event.stopPropagation();
    }

    ngOnInit(): void {
        this.isNotLabeled = !this.label;
        this.icon = this.icon ? `${this.icon}` : 'pi pi-plus';
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.label && changes.label.currentValue) {
            this.isNotLabeled = !changes.label.currentValue;
        }
    }

    /**
     * Check if the component have options for the sub menu
     *
     * @returns boolean
     * @memberof DotActionButtonComponent
     */
    isHaveOptions(): boolean {
        return !!(this.model && this.model.length);
    }

    /**
     * Handle the click to the main button
     *
     * @param {MouseEvent} $event
     * @memberof DotActionButtonComponent
     */
    buttonOnClick($event: MouseEvent): void {
        this.isHaveOptions() ? this.menu.toggle($event) : this.press.emit($event);
    }
}
