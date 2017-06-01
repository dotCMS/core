import { Component, EventEmitter, Input, Output, ViewEncapsulation, ElementRef } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
    animations: [
        trigger(
            'enterAnimation', [
                transition(':enter', [
                    style({transform: 'translateY(-10%)', opacity: 0}),
                    animate('100ms', style({transform: 'translateY(0)', opacity: 1}))
                ]),
                transition(':leave', [
                    style({transform: 'translateY(0)', opacity: 1}),
                    animate('100ms', style({transform: 'translateY(-10%)', opacity: 0}))
                ])
            ]
        )
    ],
    encapsulation: ViewEncapsulation.Emulated,
    host: {
        '(document:click)': 'handleClick($event)',
    },
    selector: 'dot-dropdown-component',
    styles: [require('./dropdown-component.scss')],
    templateUrl: 'dropdown-component.html'
})

export class DropdownComponent {
    @Input() disabled= false;
    @Input() icon = null;
    @Input() gravatar = null;
    @Input() title = null;
    @Input() position: string;
    @Input() inverse = false;
    @Output() open = new EventEmitter<any>();
    @Output() toggle = new EventEmitter<boolean>();
    @Output() close = new EventEmitter<any>();
    private show = false;

    constructor(private elementRef: ElementRef) {}

    public closeIt(): void {
        this.show = false;
    }

    // tslint:disable-next-line:no-unused-variable
    private onToggle(): void {
        this.show = !this.show;

        if (this.show) {
            this.open.emit(null);
        } else {
            this.close.emit(null);
        }

        this.toggle.emit(this.show);
    }

    // TODO: we need doing this globally for all the components that need to detect if the click was outside it.
    // tslint:disable-next-line:no-unused-variable
    private handleClick($event): void {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.elementRef.nativeElement) {
                inside = true;
            }
            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.show = false;
        }
    }
}
