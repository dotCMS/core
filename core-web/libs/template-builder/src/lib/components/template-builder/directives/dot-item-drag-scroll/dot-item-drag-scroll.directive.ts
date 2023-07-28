import { Directive, ElementRef, Host, HostListener, OnInit, Optional } from '@angular/core';

import { take } from 'rxjs/operators';

import { TemplateBuilderComponent } from '../../template-builder.component';

enum DIRECTION {
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
    private isDragging = false;
    private scrollInterval;
    private scrollDirection: DIRECTION = DIRECTION.NONE;

    @HostListener('window:mousemove', ['$event'])
    onMouseMove() {
        if (this.isDragging) {
            const windowHeight = window.innerHeight;
            const scrollSpeed = 10;
            const scrollThreshold = 50;

            const { top, bottom } = this.currentElement.getBoundingClientRect();

            const scrollUp = top - scrollThreshold < 0;
            const scrollDown = bottom + scrollThreshold > windowHeight;

            if (scrollUp) {
                // Prevents multiple intervals from being created
                if (this.scrollDirection === DIRECTION.UP) {
                    return;
                }

                this.scrollDirection = DIRECTION.UP;
                this.scrollInterval = setInterval(() => window.scrollBy(0, -scrollSpeed), 10);
            } else if (scrollDown) {
                // Prevents multiple intervals from being created
                if (this.scrollDirection === DIRECTION.DOWN) {
                    return;
                }

                this.scrollDirection = DIRECTION.DOWN;
                this.scrollInterval = setInterval(() => window.scrollBy(0, scrollSpeed), 10);
            } else {
                this.scrollDirection = DIRECTION.NONE;
                clearInterval(this.scrollInterval);
            }
        }
    }

    constructor(
        private el: ElementRef,
        @Optional() @Host() private parentComponent: TemplateBuilderComponent
    ) {
        if (!this.parentComponent) {
            console.warn(
                'dotcmsItemDragScroll directive must be used inside a dot-template-builder component'
            );
        }
    }

    ngOnInit() {
        this.parentComponent?.fullyLoaded.pipe(take(1)).subscribe(() => {
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
