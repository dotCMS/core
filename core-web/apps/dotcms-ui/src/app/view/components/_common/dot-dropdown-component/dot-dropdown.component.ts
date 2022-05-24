import {
    Component,
    EventEmitter,
    Input,
    Output,
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
    wasOpen = new EventEmitter<never>();

    @Output()
    toggle = new EventEmitter<boolean>();

    @Output()
    shutdown = new EventEmitter<never>();

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
            this.wasOpen.emit();
        } else {
            this.shutdown.emit();
        }

        this.toggle.emit(this.show);
    }
}
