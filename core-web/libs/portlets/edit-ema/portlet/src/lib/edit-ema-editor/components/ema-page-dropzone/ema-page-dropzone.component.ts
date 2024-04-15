import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    inject
} from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { DotErrorPipe } from './pipes/error/dot-error.pipe';
import { DotPositionPipe } from './pipes/position/dot-position.pipe';
import { EmaDragItem, Container } from './types';

import { PositionPayload, ClientData } from '../../../shared/models';

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
    @Input() item: EmaDragItem;
    @Output() place = new EventEmitter<PositionPayload>();

    pointerPosition: Record<string, string> = POINTER_INITIAL_POSITION;

    private readonly el = inject(ElementRef);

    /**
     * Emit place event and reset pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDrop(event: DragEvent, isEmptyContainer = false): void {
        const target = event.target as HTMLDivElement;

        const data: ClientData = JSON.parse(target.dataset.payload);
        const isTop = this.isTop(event);

        const insertPosition = isTop ? 'before' : 'after';

        const payload = <PositionPayload>{
            ...data,
            position: isEmptyContainer ? undefined : insertPosition
        };

        this.place.emit(payload);
        this.pointerPosition = POINTER_INITIAL_POSITION;
    }

    /**
     * Emit place event and reset pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDropEmptyContainer(event: DragEvent): void {
        const target = event.target as HTMLDivElement;

        const data: ClientData = JSON.parse(target.dataset.payload);

        const payload = <PositionPayload>{
            ...data
        };

        this.place.emit(payload);
        this.pointerPosition = POINTER_INITIAL_POSITION;
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

        const parentReact = this.el.nativeElement.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();
        const isTop = this.isTop(event);

        this.pointerPosition = {
            left: `${targetRect.left - parentReact.left}px`,
            width: `${targetRect.width}px`,
            opacity: '1',
            top: isTop
                ? `${targetRect.top - parentReact.top}px`
                : `${targetRect.top - parentReact.top + targetRect.height}px`,
            height: '3px'
        };
    }

    /**
     * Set the pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDragoverEmptyContainer(event: DragEvent): void {
        event.stopPropagation();
        event.preventDefault();

        const target = event.target as HTMLDivElement;

        const parentReact = this.el.nativeElement.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();

        this.pointerPosition = {
            left: `${targetRect.left - parentReact.left}px`,
            width: `${targetRect.width}px`,
            opacity: '0.1',
            top: `${targetRect.top - parentReact.top}px`,
            height: `${targetRect.height}px`
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
    private isTop(event: DragEvent): boolean {
        const target = event.target as HTMLDivElement;
        const targetRect = target.getBoundingClientRect();
        const mouseY = event.clientY;

        return mouseY < targetRect.top + targetRect.height / 2;
    }
}
