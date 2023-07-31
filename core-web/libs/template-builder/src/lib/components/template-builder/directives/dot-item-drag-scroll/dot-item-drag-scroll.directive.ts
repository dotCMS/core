import { Directive, ElementRef, Host, HostListener, OnInit, Optional } from '@angular/core';

import { take } from 'rxjs/operators';

import { TemplateBuilderComponent } from '../../template-builder.component';

export enum DIRECTION {
    UP = 'UP',
    DOWN = 'DOWN',
    NONE = 'NONE'
}

@Directive({
    selector: '[dotcmsItemDragScroll]',
    standalone: true
})
export class DotItemDragScrollDirective implements OnInit {
    private currentElement: HTMLElement;
    private scrollInterval;

    isDragging = false;
    container: HTMLElement;
    scrollDirection: DIRECTION = DIRECTION.NONE;

    @HostListener('window:mousemove', ['$event'])
    onMouseMove() {
        if (this.isDragging) {
            const containerRect = this.container.getBoundingClientRect();
            const elementRect = this.currentElement.getBoundingClientRect();
            const scrollSpeed = 10;
            const scrollThreshold = 80;

            const scrollUp = elementRect.top - containerRect.top - scrollThreshold < 0;
            const scrollDown = elementRect.bottom - containerRect.bottom + scrollThreshold > 0;

            if (scrollUp || scrollDown) {
                const direction = scrollUp ? DIRECTION.UP : DIRECTION.DOWN;

                // Prevents multiple intervals from being created
                if (this.scrollDirection === direction) {
                    return;
                }

                this.scrollDirection = direction;
                const scrollStep = () => {
                    const distance = direction === DIRECTION.UP ? -scrollSpeed : scrollSpeed;
                    this.container.scrollBy(0, distance);
                    if (this.scrollDirection === direction) {
                        requestAnimationFrame(scrollStep);
                    }
                };

                requestAnimationFrame(scrollStep);
            } else {
                this.scrollDirection = DIRECTION.NONE;
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
            this.scrollDirection = DIRECTION.NONE;
            clearInterval(this.scrollInterval);
        });
    }
}
