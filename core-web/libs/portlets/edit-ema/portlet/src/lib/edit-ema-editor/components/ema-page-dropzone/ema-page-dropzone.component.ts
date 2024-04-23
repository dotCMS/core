import { CommonModule } from '@angular/common';
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
    standalone: true,
    imports: [CommonModule, DotPositionPipe, DotErrorPipe, DotMessagePipe],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaPageDropzoneComponent {
    @Input() containers: Container[] = [];
    @Input() dragItem: EmaDragItem;

    pointerPosition: Record<string, string> = POINTER_INITIAL_POSITION;

    private readonly el = inject(ElementRef);

    protected readonly position = signal('');

    protected readonly parentRect = signal<DOMRect>(null);
    protected readonly targetRect = signal<DOMRect>(null);

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

        this.calculatePosition(event);

        const parentRect = this.parentRect();
        const targetRect = this.targetRect();

        const isEmpty = empty === 'true';

        const opacity = isEmpty ? '0.1' : '1';
        const height = isEmpty ? `${targetRect.height}px` : '3px';
        const top = this.getTop({
            targetRect,
            parentRect,
            isEmpty,
            position: this.position()
        });

        this.pointerPosition = {
            left: `${targetRect.left - parentRect.left}px`,
            width: `${targetRect.width}px`,
            opacity,
            top,
            height
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
    private calculatePosition(event: DragEvent): void {
        const target = event.target as HTMLDivElement;
        const parentRect = this.el.nativeElement.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        const mouseY = event.clientY;
        const isTop = mouseY < targetRect.top + targetRect.height / 2;

        this.position.set(isTop ? 'before' : 'after');
        this.parentRect.set(parentRect);
        this.targetRect.set(targetRect);
    }

    private getTop({
        targetRect,
        parentRect,
        isEmpty,
        position
    }: {
        targetRect: DOMRect;
        isEmpty: boolean;
        position: string;
        parentRect: DOMRect;
    }) {
        if (isEmpty) {
            return `${targetRect.top - parentRect.top}px`;
        } else {
            return position === 'before'
                ? `${targetRect.top - parentRect.top}px`
                : `${targetRect.top - parentRect.top + targetRect.height}px`;
        }
    }
}
