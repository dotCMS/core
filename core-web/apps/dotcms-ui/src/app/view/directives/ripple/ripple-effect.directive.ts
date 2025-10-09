import { Directive, ElementRef, HostBinding, HostListener, Renderer2, inject } from '@angular/core';

import { ColorUtil } from '../../../api/util/ColorUtil';

/**
 * Directives to dotMdRipple effects.
 * How to use:
 * <code>
 *  <button dotMdRipple pButton></button>
 * </code>
 */
@Directive({
    selector: '[dotMdRipple]',
    standalone: true
})
export class DotRippleEffectDirective {
    private host = inject(ElementRef);
    private renderer2 = inject(Renderer2);
    private colorUtil = inject(ColorUtil);

    private static readonly WHITE_COLOR = 'rgba(255, 255, 255, 0.4)';
    private static readonly EFFECT_DEFAULT_COLOR = 'rgba(0, 0, 0, 0.2)';

    @HostBinding('style.overflow')
    overflow = 'hidden';

    @HostBinding('style.position')
    position = 'relative';

    private rippleElement: HTMLElement;
    private rippleSize: RippleSize;
    private hostNativeElement: HTMLElement = this.host.nativeElement;

    @HostListener('click', ['$event'])
    onClick(event: MouseEvent): void {
        if (!this.rippleElement) {
            this.createRippleElement();
        }

        this.setRipplePosition(event);
        this.animateRipple();
    }

    private animateRipple(): void {
        this.renderer2.addClass(this.rippleElement, 'animate');

        setTimeout(() => {
            this.renderer2.addClass(this.rippleElement, 'animate');
        }, 1);
    }

    private createRippleElement(): void {
        this.rippleElement = this.renderer2.createElement('div');
        this.renderer2.appendChild(this.hostNativeElement, this.rippleElement);
        this.rippleSize = this.getRippleSize();
        this.renderer2.addClass(this.rippleElement, 'ripple-effect');
        this.renderer2.setStyle(this.rippleElement, 'width', `${this.rippleSize.width}px`);
        this.renderer2.setStyle(this.rippleElement, 'height', `${this.rippleSize.height}px`);
        this.renderer2.setStyle(this.rippleElement, 'background', this.getRippleColor());
    }

    private getRippleColor(): string {
        const hostBackgroundColor = window.getComputedStyle(
            this.hostNativeElement,
            null
        ).backgroundColor;
        const isBright = this.colorUtil.isBrightness(hostBackgroundColor);

        return isBright
            ? DotRippleEffectDirective.WHITE_COLOR
            : DotRippleEffectDirective.EFFECT_DEFAULT_COLOR;
    }

    private getRipplePosition(event: MouseEvent): RipplePosition {
        const btnOffset = this.hostNativeElement.getBoundingClientRect();
        const xPos = event.pageX - btnOffset.left;
        const yPos = event.pageY - btnOffset.top;

        return {
            x: xPos - this.rippleSize.width / 2,
            y: yPos - this.rippleSize.height / 2
        };
    }

    private getRippleSize(): RippleSize {
        const btnOffset = this.host.nativeElement.getBoundingClientRect();
        const rippleSize =
            Math.sqrt(btnOffset.width * btnOffset.width + btnOffset.height * btnOffset.height) * 2 +
            2;

        return {
            height: rippleSize,
            width: rippleSize
        };
    }

    private setRipplePosition(event: MouseEvent): void {
        const ripplePosition: RipplePosition = this.getRipplePosition(event);
        this.renderer2.setStyle(this.rippleElement, 'top', `${ripplePosition.y}px`);
        this.renderer2.setStyle(this.rippleElement, 'left', `${ripplePosition.x}px`);
    }
}

interface RippleSize {
    height: number;
    width: number;
}

interface RipplePosition {
    x: number;
    y: number;
}
