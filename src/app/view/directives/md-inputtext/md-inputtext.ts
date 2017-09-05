import { Directive, ElementRef, Renderer, Input, OnChanges } from '@angular/core';

/**
 * Directives material design floating label textfield.
 * How to use:
 * <code>
 * <span md-inputtext label="Placeholder">
 *   <input type="text" pInputText />
 * </span>
 * </code>
 */
@Directive({
    selector: '[md-inputtext]'
})
export class MaterialDesignTextfield implements OnChanges {
    @Input() private label: string;
    private hostNativeElement: HTMLElement;
    private floatingLabel: HTMLElement;

    constructor(private host: ElementRef, private renderer: Renderer) {
        this.hostNativeElement = host.nativeElement;
        this.hostNativeElement.className = 'md-inputtext';
    }

    ngOnChanges(): void {
        this.setLabel();
    }

    setLabel(): void {
        if (!this.floatingLabel) {
            this.floatingLabel = this.renderer.createElement(this.hostNativeElement, 'label');
        }
        this.floatingLabel.innerHTML = this.label;
    }
}
