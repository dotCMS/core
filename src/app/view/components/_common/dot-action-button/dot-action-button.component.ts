import { Component, Input, ViewChild, OnInit, HostBinding, SimpleChanges, EventEmitter, Output } from '@angular/core';
import { MenuItem, Menu } from 'primeng/primeng';
import { OnChanges } from '@angular/core/src/metadata/lifecycle_hooks';

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
    @ViewChild('menu') menu: Menu;

    @Input() disabled: boolean;
    @Input() icon: string;
    @Input() label: string;
    @Input() model: MenuItem[];
    @Input() selected: boolean;

    @Output() onClick: EventEmitter<any> = new EventEmitter();

    @HostBinding('class.action-button--no-label') isNotLabeled = true;

    ngOnInit(): void {
        this.isNotLabeled = !this.label;
        this.icon = this.icon ? `fa ${this.icon}` : 'fa fa-plus';
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.label && changes.label.currentValue) {
            this.isNotLabeled = !changes.label.currentValue;
        }
    }

    /**
     * Check if the component have options for the sub menu
     *
     * @returns {boolean}
     * @memberof DotActionButtonComponent
     */
    isHaveOptions(): boolean {
        return !!(this.model && this.model.length);
    }

    /**
     * Handle the click to the main button
     *
     * @param {any} $event
     * @memberof DotActionButtonComponent
     */
    buttonOnClick($event): void {
        this.isHaveOptions() ? this.menu.toggle($event) : this.onClick.emit($event);
    }
}
