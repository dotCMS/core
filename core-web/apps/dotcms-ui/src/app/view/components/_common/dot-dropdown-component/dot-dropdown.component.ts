import { animate, style, transition, trigger } from '@angular/animations';
import {
    Component,
    computed,
    ElementRef,
    HostListener,
    inject,
    input,
    signal,
    output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

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
    templateUrl: 'dot-dropdown.component.html',
    standalone: true,
    imports: [ButtonModule]
})
export class DotDropdownComponent {
    /** Reference to the component's DOM element for click detection */
    readonly #elementRef = inject(ElementRef);

    /**
     * Controls whether the dropdown trigger button is disabled.
     * When true, prevents user interaction with the dropdown.
     * @default false
     */
    $disabled = input(false, { alias: 'disabled' });

    /**
     * Icon to display on the dropdown trigger button.
     * Accepts PrimeNG icon classes or null for no icon.
     * @default null
     */
    $icon = input(null, { alias: 'icon' });

    /**
     * Title text to display on the dropdown trigger button.
     * Can be null if only an icon is desired.
     * @default null
     */
    $title = input(null, { alias: 'title' });

    /**
     * Position of the dropdown content relative to the trigger button.
     * 'left' aligns content to the left edge, 'right' to the right edge.
     * @default 'left'
     */
    $position = input<'left' | 'right'>('left', { alias: 'position' });

    /**
     * Controls the visual styling theme of the dropdown.
     * When true, applies inverted color scheme.
     * @default false
     */
    $inverted = input(false, { alias: 'inverted' });

    /**
     * Computed CSS style object for positioning the dropdown content.
     * Dynamically sets left or right positioning based on $position input.
     * @returns CSS style object with positioning property
     */
    $style = computed(() => {
        return {
            [this.$position()]: '0'
        };
    });

    /**
     * Computed boolean indicating if the dropdown should be in disabled state.
     * Returns true only when both disabled is true AND an icon is present.
     * @returns true if dropdown should be disabled with icon, false otherwise
     */
    $disabledState = computed(() => {
        const icon = this.$icon();
        const disabled = this.$disabled();

        return disabled && icon ? true : false;
    });

    /**
     * Event emitted when the dropdown is opened.
     * Useful for triggering actions when dropdown becomes visible.
     */
    wasOpen = output<void>();

    /**
     * Event emitted whenever the dropdown visibility state changes.
     * Emits the current visibility state (true for open, false for closed).
     */
    toggle = output<boolean>();

    /**
     * Event emitted when the dropdown is closed.
     * Useful for cleanup actions when dropdown becomes hidden.
     */
    shutdown = output<void>();

    /**
     * Signal tracking the current visibility state of the dropdown.
     * true when dropdown is open, false when closed.
     * @default false
     */
    $show = signal(false);

    /**
     * Host listener that handles clicks outside the dropdown component.
     * Automatically closes the dropdown when user clicks outside of it.
     * Traverses the DOM tree to determine if click occurred inside component.
     *
     * @param $event - The mouse click event from the document
     */
    @HostListener('document:click', ['$event'])
    handleClick($event) {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.#elementRef.nativeElement) {
                inside = true;
            }

            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.$show.set(false);
        }
    }

    /**
     * Programmatically closes the dropdown by setting visibility to false.
     * Can be called from parent components or internal logic to hide dropdown.
     */
    closeIt(): void {
        this.$show.set(false);
    }

    /**
     * Toggles the dropdown visibility state and emits appropriate events.
     * - Flips the current $show state
     * - Emits wasOpen when dropdown opens
     * - Emits shutdown when dropdown closes
     * - Always emits toggle with current state
     */
    onToggle(): void {
        this.$show.update((value) => !value);

        if (this.$show()) {
            this.wasOpen.emit();
        } else {
            this.shutdown.emit();
        }

        this.toggle.emit(this.$show());
    }
}
