import { describe, expect, it } from '@jest/globals';
import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { Component, ElementRef } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DIRECTION, DotItemDragScrollDirective } from './dot-item-drag-scroll.directive';

import { DotTemplateBuilderStore } from '../../store/template-builder.store';
import { TemplateBuilderComponent } from '../../template-builder.component';
import { DOT_MESSAGE_SERVICE_TB_MOCK } from '../../utils/mocks';

const mockRect = {
    top: 120,
    bottom: 100,
    x: 146,
    y: 50,
    width: 440,
    height: 240,
    right: 586,
    left: 146,
    toJSON: jest.fn()
};

const containerElementMock = () => {
    const el = document.createElement('div');
    el.getBoundingClientRect = jest.fn();
    el.scrollBy = jest.fn();

    return el;
};

/**
 * Mock of an element inside the gridstack
 *
 * @class MockGridStackElementComponent
 */
@Component({
    selector: 'dotcms-grid-stack-element',
    template: '<div>Element</div>'
})
class MockGridStackElementComponent {
    constructor(public el: ElementRef) {
        this.el.nativeElement.ddElement = {
            on: jest.fn()
        };
    }
}

describe('DotItemDragScrollDirective', () => {
    let spectator: SpectatorDirective<DotItemDragScrollDirective>;

    const createDirective = createDirectiveFactory({
        directive: DotItemDragScrollDirective,
        imports: [DotMessagePipe],
        declarations: [MockComponent(TemplateBuilderComponent), MockGridStackElementComponent],
        providers: [
            TemplateBuilderComponent,
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DotTemplateBuilderStore,
            DialogService
        ]
    });

    beforeEach(() => {
        spectator = createDirective(`<dotcms-template-builder-lib>
        <dotcms-grid-stack-element dotcmsItemDragScroll></dotcms-grid-stack-element>
        </dotcms-template-builder-lib>`);

        spectator.directive.container = containerElementMock();
        spectator.directive.currentElement = spectator.directive.el.nativeElement;
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.directive).toBeTruthy();
    });

    it('should called "on" method wiht "dragstart" and "dragstop" of the ddElement when fullyLoaded event is emitted', () => {
        const ddElementSpy = jest.spyOn(spectator.directive.el.nativeElement.ddElement, 'on');
        spectator.directive.parentComponent.fullyLoaded.emit();

        expect(ddElementSpy).toHaveBeenCalledTimes(2);
        expect(ddElementSpy).toHaveBeenCalledWith('dragstart', expect.any(Function));
        expect(ddElementSpy).toHaveBeenCalledWith('dragstop', expect.any(Function));
    });

    it('should not scroll if isDragging is false', () => {
        const scrollBySpy = jest.spyOn(spectator.directive.container, 'scrollBy');
        spectator.directive.isDragging = false;
        spectator.directive.onMouseMove();
        expect(spectator.directive.scrollDirection).toBe(DIRECTION.NONE);
        expect(scrollBySpy).not.toHaveBeenCalled();
    });

    it('should scroll up if the element is close to the top of the container', () => {
        const spy = jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => 1);
        jest.spyOn(spectator.directive.currentElement, 'getBoundingClientRect').mockReturnValue({
            ...mockRect,
            top: 0
        });
        jest.spyOn(spectator.directive.container, 'getBoundingClientRect').mockReturnValue({
            ...mockRect,
            top: 0
        });

        spectator.directive.isDragging = true;
        spectator.directive.onMouseMove();

        expect(spectator.directive.scrollDirection).toBe(DIRECTION.UP);
        expect(spy).toHaveBeenCalled();
    });

    it('should scroll down if the element is close to the bottom of the container', () => {
        jest.spyOn(spectator.directive.currentElement, 'getBoundingClientRect').mockReturnValue({
            ...mockRect,
            top: 500,
            bottom: 0
        });
        jest.spyOn(spectator.directive.container, 'getBoundingClientRect').mockReturnValue({
            ...mockRect,
            top: 100,
            bottom: 0
        });

        spectator.directive.isDragging = true;
        spectator.directive.onMouseMove();

        expect(spectator.directive.scrollDirection).toBe(DIRECTION.DOWN);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
});
