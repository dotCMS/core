import { Subscription } from 'rxjs';

import {
    AfterViewInit,
    Directive,
    ElementRef,
    OnDestroy,
    Optional,
    Renderer2,
    Self
} from '@angular/core';
import { NgControl } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

@Directive({
    selector: '[dotClipboard]',
    standalone: true
})
export class DotClipboardDirective implements AfterViewInit, OnDestroy {
    tooltipText = '';
    valueChangesSubscription: Subscription;

    constructor(
        private el: ElementRef,
        private renderer: Renderer2,
        private dotMessageService: DotMessageService,
        @Optional() @Self() public ngControl: NgControl
    ) {}
    //
    ngAfterViewInit() {
        this.createHTML();

        // Now you can access the ngControl
        if (this.ngControl) {
            this.valueChangesSubscription = this.ngControl.valueChanges.subscribe((value) => {
                const copyButton = this.el.nativeElement.parentNode.querySelector('.p-button');
                if (value && value.trim() !== '') {
                    this.renderer.removeClass(copyButton, 'hidden');
                } else {
                    this.renderer.addClass(copyButton, 'hidden');
                }
            });
        }
    }

    ngOnDestroy() {
        // Clean up the subscription when the directive is destroyed
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }

    private createHTML() {
        const wrapperDiv = this.renderer.createElement('div');
        this.renderer.setStyle(wrapperDiv, 'position', 'relative');
        this.renderer.insertBefore(
            this.el.nativeElement.parentNode,
            wrapperDiv,
            this.el.nativeElement,
            true
        );

        // Append the textarea to the wrapper div
        this.renderer.appendChild(wrapperDiv, this.el.nativeElement);

        // Apply styles to the textarea
        this.renderer.setStyle(this.el.nativeElement, 'width', '100%');
        this.renderer.setStyle(this.el.nativeElement, 'resize', 'none');

        // Create the copy button
        const copyButton = this.renderer.createElement('button');
        this.renderer.addClass(copyButton, 'p-button');
        this.renderer.addClass(copyButton, 'p-button-sm');
        this.renderer.addClass(copyButton, 'p-button-icon-only');
        this.renderer.addClass(copyButton, 'p-button-rounded');
        this.renderer.addClass(copyButton, 'p-button-text');

        this.renderer.setAttribute(copyButton, 'pTooltip', this.dotMessageService.get('Copied'));
        // this.renderer.setAttribute(icon, 'pTooltip', this.tooltipText);
        // this.renderer.setAttribute(copyButton, 'tooltipPosition', 'bottom');
        // this.renderer.setAttribute(copyButton, 'hideDelay', '800');
        // this.renderer.setAttribute(copyButton, 'appendTo', 'body');

        this.renderer.setAttribute(copyButton, 'type', 'button');
        this.renderer.appendChild(wrapperDiv, copyButton);

        // this.renderer.setStyle(copyButton, 'background', 'var(--color-palette-primary-200)');

        // Create the icon element
        const icon = this.renderer.createElement('i');
        this.renderer.addClass(icon, 'pi');
        this.renderer.addClass(icon, 'pi-copy');
        //this.renderer.setStyle(icon, 'color', 'var(--color-palette-primary-500)');

        this.renderer.appendChild(copyButton, icon);

        // Position the button
        this.renderer.setStyle(copyButton, 'position', 'absolute');
        this.renderer.setStyle(copyButton, 'bottom', '0.5rem');
        this.renderer.setStyle(copyButton, 'right', '0.5rem');

        // Track changes in textarea value
        this.renderer.listen(this.el.nativeElement, 'input', () => {
            const copyButton = this.el.nativeElement.parentNode.querySelector('.p-button');
            if (this.el.nativeElement.value.trim() !== '') {
                this.renderer.removeClass(copyButton, 'hidden');
            } else {
                this.renderer.addClass(copyButton, 'hidden');
            }
        });

        // Add click event listener to copy button
        this.renderer.listen(copyButton, 'click', () => {
            navigator.clipboard.writeText(this.el.nativeElement.value);

            //this.showTooltip('success', 'Text copied!', copyButton);

            this.tooltipText = this.dotMessageService.get('Copied');
            // this.renderer.setAttribute(
            //     copyButton,
            //     'pTooltip',
            //     this.dotMessageService.get('Copied')
            // );
            setTimeout(() => {
                this.renderer.setAttribute(copyButton, 'pTooltip', null);
            }, 1000);
        });
    }

    // // Function to show tooltip
    // showTooltip(severity: string, detail: string, element: HTMLElement) {
    //     this.tooltipService.add({
    //         target: element,
    //         message: detail,
    //         severity: severity,
    //         life: 2000 // 2 seconds
    //     });
    //}
}
