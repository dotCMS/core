import {
    Directive,
    ElementRef,
    Input,
    HostListener,
    Renderer,
    AnimationPlayer,
    AnimationStyles,
    AnimationKeyframe
} from '@angular/core';
import { ColorUtil } from '../../../api/util/ColorUtil';

/**
 * Directives to ripple effects.
 * How to use:
 * <code>
 *  <button ripple pButton id="login-component-login-submit-button" *ngIf="!isLoginInProgress" (click)="logInUser()" [label]="loginButton"></button>
 * </code>
 */
@Directive({
    host: {
        '[style.overflow]': '"hidden"',
        '[style.position]': '"relative"'
    },
    selector: '[ripple]'
})
export class DotRippleEffectDirective {
    private static readonly WHITE_COLOR = 'rgba(255, 255, 255, 0.4)';
    private static readonly EFFECT_DEFAULT_COLOR = 'rgba(0, 0, 0, 0.2)';
    private rippleElement: HTMLElement;
    private rippleSize: RippleSize;
    private hostNativeElement: HTMLElement;

    constructor(private host: ElementRef, private renderer: Renderer, private colorUtil: ColorUtil) {
        this.hostNativeElement = host.nativeElement;
    }

    @HostListener('click', ['$event'])
    onClick(event: MouseEvent): void {
        if (!this.rippleElement) {
             this.createRippleElement();
        }

        this.setRipplePosition(event);
        this.animateRipple();
    }

    private animateRipple(): void {
        this.renderer.setElementClass(this.rippleElement, 'animate', false);
        setTimeout(() => {
            this.renderer.setElementClass(this.rippleElement, 'animate', true);
        }, 1);
    }

    private createRippleElement(): void {
        this.rippleElement = this.renderer.createElement(this.hostNativeElement, 'div');
        this.rippleSize = this.getRippleSize();
        this.renderer.setElementClass(this.rippleElement, 'ripple-effect', true);
        this.renderer.setElementStyle(this.rippleElement, 'width', `${this.rippleSize.width}px`);
        this.renderer.setElementStyle(this.rippleElement, 'height', `${this.rippleSize.height}px`);
        this.renderer.setElementStyle(this.rippleElement, 'background', this.getRippleColor());
    }

    private getRippleColor(): string {
        let hostBackgroundColor = window.getComputedStyle(this.hostNativeElement, null).backgroundColor;
        let isBright = this.colorUtil.isBrightness(hostBackgroundColor);

        return isBright ? DotRippleEffectDirective.WHITE_COLOR : DotRippleEffectDirective.EFFECT_DEFAULT_COLOR;
    }

    private getRipplePosition(event: MouseEvent): RipplePosition {
        let btnOffset = this.hostNativeElement.getBoundingClientRect();
        let xPos = event.pageX - btnOffset.left;
        let yPos = event.pageY - btnOffset.top;

        return {
            x: (xPos - (this.rippleSize.width / 2)),
            y: (yPos - (this.rippleSize.height / 2))
        };
    }

    private getRippleSize(): RippleSize {
        let btnOffset = this.host.nativeElement.getBoundingClientRect();
        let rippleSize = Math.sqrt(btnOffset.width * btnOffset.width +
          btnOffset.height * btnOffset.height) * 2 + 2;

        return {
            height: rippleSize,
            width: rippleSize
        };
    }

    private setRipplePosition(event: MouseEvent): void {
        let ripplePosition: RipplePosition = this.getRipplePosition(event);
        this.renderer.setElementStyle(this.rippleElement, 'top', `${ripplePosition.y}px`);
        this.renderer.setElementStyle(this.rippleElement, 'left',  `${ripplePosition.x}px`);
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