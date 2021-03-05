import {
    Component,
    EventEmitter,
    Input,
    Output,
    ViewEncapsulation,
    ElementRef,
    HostListener,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
    animations: [
        trigger('enterAnimation', [
            transition(':enter', [
                style({ transform: 'translateY(-10%)', opacity: 0 }),
                animate('100ms', style({ transform: 'translateY(0)', opacity: 1 }))
            ]),
            transition(':leave', [
                style({ transform: 'translateY(0)', opacity: 1 }),
                animate('100ms', style({ transform: 'translateY(-10%)', opacity: 0 }))
            ])
        ])
    ],
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-dropdown-component',
    styleUrls: ['./dot-dropdown.component.scss'],
    templateUrl: 'dot-dropdown.component.html'
})
export class DotDropdownComponent implements OnChanges {
    @Input()
    disabled = false;
    @Input()
    icon = null;
    @Input()
    gravatar = null;
    @Input()
    title = null;
    @Input()
    position: string;
    @Input()
    inverted = false;
    @Output()
    open = new EventEmitter<any>();
    @Output()
    toggle = new EventEmitter<boolean>();
    @Output()
    close = new EventEmitter<any>();
    show = false;

    constructor(private elementRef: ElementRef) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.disabled && this.icon) {
            this.disabled = changes.disabled.currentValue ? true : null;
        }
    }

    @HostListener('document:click', ['$event'])
    handleClick($event) {
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

    closeIt(): void {
        this.show = false;
    }

    onToggle(): void {
        this.show = !this.show;

        if (this.show) {
            this.open.emit(null);
        } else {
            this.close.emit(null);
        }

        this.toggle.emit(this.show);
    }
}
