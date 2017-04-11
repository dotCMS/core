import { Component, EventEmitter, Input, Output, ViewEncapsulation, ElementRef, trigger, style, state, transition, animate } from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    host: {
        '(document:click)': 'handleClick($event)',
    },

    selector: 'dot-dropdown-component',
    styles: [require('./dropdown-component.scss')],
    templateUrl: 'dropdown-component.html',
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
  ]
})

export class DropdownComponent {
    @Input() disabled= false;
    @Input() icon = null;
    @Input() title = null;
    @Input() alignRight = false;
    @Input() inverse = false;

    @Output() open = new EventEmitter<any>();
    @Output() toggle = new EventEmitter<boolean>();
    @Output() close = new EventEmitter<any>();

    private show = false;

    constructor(private elementRef: ElementRef) {}

    public closeIt(): void {
        this.show = false;
    }

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
