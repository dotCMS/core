import {
    Component,
    Input,
    ViewChild,
    OnInit,
    HostBinding,
    SimpleChanges,
    EventEmitter,
    Output,
    HostListener,
    OnChanges
} from '@angular/core';
import { Menu } from 'primeng/menu';
import { MenuItem } from 'primeng/api';

/**
 * The ActionButtonComponent is a configurable button with
 * options to add to the primary actions in the portlets.
 * @export
 * @class ActionButtonComponent
 */
@Component({
    selector: 'dot-action-button',
    styleUrls: ['./dot-action-button.component.scss'],
    templateUrl: 'dot-action-button.component.html'
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
    click: EventEmitter<any> = new EventEmitter();

    @HostBinding('class.action-button--no-label')
    isNotLabeled = true;

    @HostListener('click', ['$event'])
    public onClick(event: any): void {
        event.stopPropagation();
    }

    ngOnInit(): void {
        this.isNotLabeled = !this.label;
        this.icon = this.icon ? `${this.icon}` : 'add';
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
     * @param any $event
     * @memberof DotActionButtonComponent
     */
    buttonOnClick($event): void {
        this.isHaveOptions() ? this.menu.toggle($event) : this.click.emit($event);
    }
}
