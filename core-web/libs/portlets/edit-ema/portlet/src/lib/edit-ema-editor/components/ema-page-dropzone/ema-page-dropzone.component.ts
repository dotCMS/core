import { NgStyle, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    Input,
    inject,
    signal
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotErrorPipe } from './pipes/error/dot-error.pipe';
import { DotPositionPipe } from './pipes/position/dot-position.pipe';
import { EmaDragItem, Container } from './types';

const POINTER_INITIAL_POSITION = {
    left: '0',
    width: '0',
    opacity: '0',
    top: '0'
};

@Component({
    selector: 'dot-ema-page-dropzone',
    imports: [DotPositionPipe, DotErrorPipe, DotMessagePipe, NgStyle, NgTemplateOutlet],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaPageDropzoneComponent {
    @Input() containers: Container[] = [];
    @Input() dragItem: EmaDragItem;
    @Input() zoomLevel = 1;

    pointerPosition: Record<string, string> = POINTER_INITIAL_POSITION;

    private readonly el = inject(ElementRef);

    protected readonly $positionData = signal({
        position: '',
        parentRect: null,
        targetRect: null
    });

    /**
     * Set the pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDragover(event: DragEvent): void {
        const target = event.target as HTMLDivElement;

        const { empty, dropzone } = target.dataset;

        // We can dragover the error zone
        if (dropzone !== 'true') {
            return;
        }

        this.setPositionData(event);

        const { parentRect, targetRect } = this.$positionData();

        const isEmpty = empty === 'true';

        const opacity = isEmpty ? '0.1' : '1';
        // Adjust coordinates for zoom level
        const adjustedHeight = isEmpty ? targetRect.height / this.zoomLevel : 3;
        const top = this.getTop(isEmpty);

        this.pointerPosition = {
            left: `${(targetRect.left - parentRect.left) / this.zoomLevel}px`,
            width: `${targetRect.width / this.zoomLevel}px`,
            opacity,
            top,
            height: `${adjustedHeight}px`
        };
    }

    /**
     * Check pointer position
     *
     * @private
     * @param {DragEvent} event
     * @return {*}  {boolean}
     * @memberof EmaPageDropzoneComponent
     */
    private setPositionData(event: DragEvent): void {
        const target = event.target as HTMLDivElement;
        const parentRect = this.el.nativeElement.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        const mouseY = event.clientY;
        const isTop = mouseY < targetRect.top + targetRect.height / 2;

        this.$positionData.set({
            position: isTop ? 'before' : 'after',
            parentRect,
            targetRect
        });
    }

    private getTop(isEmpty: boolean): string {
        const { parentRect, targetRect, position } = this.$positionData();

        // Adjust coordinates for zoom level
        // getBoundingClientRect() returns viewport coordinates, but we need
        // coordinates relative to the transformed parent, so we adjust by zoom
        const adjustedTop = (targetRect.top - parentRect.top) / this.zoomLevel;
        const adjustedHeight = targetRect.height / this.zoomLevel;

        if (isEmpty) {
            return `${adjustedTop}px`;
        }

        return position === 'before'
            ? `${adjustedTop}px`
            : `${adjustedTop + adjustedHeight}px`;
    }
}
