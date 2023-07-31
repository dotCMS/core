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
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_TB_MOCK
            },
            DotTemplateBuilderStore,
            DialogService
        ]
    });

    beforeEach(() => {
        spectator = createDirective(`<dotcms-template-builder>
        <dotcms-grid-stack-element dotcmsItemDragScroll></dotcms-grid-stack-element>
        </dotcms-template-builder>`);
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
        spectator.directive.isDragging = false;
        spectator.directive.onMouseMove();
        expect(spectator.directive.scrollDirection).toBe(DIRECTION.NONE);
        // expect(spectator.directive.el.nativeElement.scrollBy).not.toHaveBeenCalled();
    });
});
