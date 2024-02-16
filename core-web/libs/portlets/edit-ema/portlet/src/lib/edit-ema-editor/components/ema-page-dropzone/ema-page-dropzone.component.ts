import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotErrorPipe } from './pipes/error/dot-error.pipe';
import { DotPositionPipe } from './pipes/position/dot-position.pipe';
import { Row, EmaDragItem } from './types';

import { PositionPayload, ClientData } from '../../../shared/models';

@Component({
    selector: 'dot-ema-page-dropzone',
    standalone: true,
    imports: [CommonModule, DotPositionPipe, DotErrorPipe, DotMessagePipe],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaPageDropzoneComponent {
    @Input() rows: Row[] = [];
    @Input() item: EmaDragItem;
    @Output() place = new EventEmitter<PositionPayload>();

    pointerPosition: Record<string, string> = {
        left: '0',
        width: '0',
        opacity: '0',
        top: '0'
    };

    constructor(private readonly el: ElementRef) {}

    /**
     * Emit place event and reset pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDrop(event: DragEvent): void {
        const target = event.target as HTMLDivElement;
        const data: ClientData = JSON.parse(target.dataset.payload);
        const isTop = this.isTop(event);

        const payload = <PositionPayload>{
            ...data,
            position: isTop ? 'before' : 'after'
        };

        this.place.emit(payload);
        this.pointerPosition = {
            left: '0',
            width: '0',
            opacity: '0',
            top: '0'
        };
    }

    /**
     * Set the pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDragover(event: DragEvent): void {
        event.stopPropagation();
        event.preventDefault();

        const target = event.target as HTMLDivElement;
        const { type = '' } = target.dataset;

        if (type !== 'contentlet') {
            return;
        }

        const parentReact = this.el.nativeElement.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        const isTop = this.isTop(event);

        this.pointerPosition = {
            left: `${targetRect.left - parentReact.left}px`,
            width: `${targetRect.width}px`,
            opacity: '1',
            top: isTop
                ? `${targetRect.top - parentReact.top}px`
                : `${targetRect.top - parentReact.top + targetRect.height}px`
        };
    }

    private isTop(event: DragEvent): boolean {
        const target = event.target as HTMLDivElement;
        const targetRect = target.getBoundingClientRect();
        const mouseY = event.clientY;

        return mouseY < targetRect.top + targetRect.height / 2;
    }
}
