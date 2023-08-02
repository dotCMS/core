import { Directive, ElementRef, Host, HostListener, OnInit, Optional } from '@angular/core';

import { take } from 'rxjs/operators';

import { SCROLL_DIRECTION } from '../../models/models';
import { TemplateBuilderComponent } from '../../template-builder.component';

@Directive({
    selector: '[dotcmsItemDragScroll]',
    standalone: true
})
export class DotItemDragScrollDirective implements OnInit {
    private scrollInterval;

    isDragging = false;
    container: HTMLElement;
    currentElement: HTMLElement;
    scrollDirection: SCROLL_DIRECTION = SCROLL_DIRECTION.NONE;

    @HostListener('window:mousemove', ['$event'])
    onMouseMove() {
        if (this.isDragging) {
            const containerRect = this.container.getBoundingClientRect();
            const elementRect = this.currentElement.getBoundingClientRect();
            const scrollSpeed = 10;
            const scrollThreshold = 100;

            const scrollUp = elementRect.top - containerRect.top - scrollThreshold < 0;
            const scrollDown = elementRect.bottom - containerRect.bottom + scrollThreshold > 0;

            if (scrollUp || scrollDown) {
                const direction = scrollUp ? SCROLL_DIRECTION.UP : SCROLL_DIRECTION.DOWN;

                // Prevents multiple intervals from being created
                if (this.scrollDirection === direction) {
                    return;
                }

                this.scrollDirection = direction;
                const scrollStep = () => {
                    const distance = direction === SCROLL_DIRECTION.UP ? -scrollSpeed : scrollSpeed;
                    this.container.scrollBy(0, distance);
                    if (this.scrollDirection === direction) {
                        requestAnimationFrame(scrollStep);
                    }
                };

                requestAnimationFrame(scrollStep);
            } else {
                this.scrollDirection = SCROLL_DIRECTION.NONE;
            }
        }
    }

    constructor(
        public el: ElementRef,
        @Optional() @Host() public parentComponent: TemplateBuilderComponent
    ) {
        if (!this.parentComponent) {
            console.warn(
                'dotcmsItemDragScroll directive must be used inside a dot-template-builder component'
            );
        }
    }

    ngOnInit() {
        this.parentComponent?.fullyLoaded.pipe(take(1)).subscribe(() => {
            this.container = this.parentComponent.templateContaniner;
            this.listenDragEvents();
        });
    }

    private listenDragEvents() {
        this.el.nativeElement?.ddElement.on('dragstart', ({ target }) => {
            this.currentElement = target.ddElement.ddDraggable?.helper || this.el.nativeElement;
            this.isDragging = true;
        });

        this.el.nativeElement?.ddElement.on('dragstop', () => {
            this.isDragging = false;
            this.scrollDirection = SCROLL_DIRECTION.NONE;
            clearInterval(this.scrollInterval);
        });
    }
}
