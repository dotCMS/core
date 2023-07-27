import { Directive, ElementRef, Host, HostListener, Input, Optional } from '@angular/core';

import { TemplateBuilderComponent } from '../../template-builder.component';

@Directive({
    selector: '[dotcmsItemDragScroll]',
    standalone: true
})
export class DotItemDragScrollDirective {
    @Input() dragClass: string;

    private scrollInterval;
    private scrollDirection: string;

    @HostListener('window:mousemove', ['$event'])
    onMouseMove() {
        const isDragging = this.el.nativeElement.classList.contains(this.dragClass);
        if (isDragging) {
            const windowHeight = window.innerHeight;
            const scrollSpeed = 10;
            const topRect = this.el.nativeElement.getBoundingClientRect().top;
            const bottomRect = this.el.nativeElement.getBoundingClientRect().bottom;

            if (topRect - 50 < 0) {
                // Prevents multiple intervals from being created
                if (this.scrollDirection === 'up') {
                    return;
                }

                this.scrollDirection = 'up';
                this.scrollInterval = setInterval(() => {
                    window.scrollBy(0, -scrollSpeed);
                }, 10);
            } else if (bottomRect > windowHeight - 10) {
                // Prevents multiple intervals from being created
                if (this.scrollDirection === 'down') {
                    return;
                }

                this.scrollDirection = 'down';
                this.scrollInterval = setInterval(() => {
                    window.scrollBy(0, scrollSpeed);
                }, 10);
            } else {
                clearInterval(this.scrollInterval);
                this.scrollDirection = '';
            }
        }
    }

    @HostListener('window:mouseup')
    onMouseUp() {
        clearInterval(this.scrollInterval);
    }

    constructor(
        private el: ElementRef,
        @Optional() @Host() private parentComponent: TemplateBuilderComponent
    ) {}
}
