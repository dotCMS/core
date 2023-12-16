import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output
} from '@angular/core';

import { ActionPayload } from '../../shared/models';

interface Contentlets {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: ActionPayload;
}

interface Container {
    x: number;
    y: number;
    width: number;
    height: number;
    contentlets: Contentlets[];
    payload: ActionPayload;
}

interface Column {
    x: number;
    y: number;
    width: number;
    height: number;
    containers: Container[];
}

export interface Row {
    x: number;
    y: number;
    width: number;
    height: number;
    columns: Column[];
}

@Component({
    selector: 'dot-ema-page-dropzone',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaPageDropzoneComponent {
    @Input() rows: Row[] = [];
    @Output() place = new EventEmitter<ActionPayload>();

    pointerPosition: Record<string, string> = {
        left: '0',
        width: '0',
        opacity: '0',
        top: '0'
    };

    constructor(private readonly el: ElementRef) {}

    getStyle(
        item: Row | Column | Container | Contentlets,
        border = 'black'
    ): Record<string, string> {
        return {
            position: 'absolute',
            left: `${item.x}px`,
            top: `${item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`,
            border: `1px solid ${border}`
        };
    }

    onDrop(event: DragEvent): void {
        const target = event.target as HTMLDivElement;
        const data = JSON.parse(target.dataset.payload);
        const targetRect = (event.target as HTMLElement).getBoundingClientRect();
        const mouseY = event.clientY;
        const isTop = mouseY < targetRect.top + targetRect.height / 2;

        const payload = <ActionPayload>{
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

    onDragover(event) {
        const target = event.target as HTMLDivElement;

        if (target.dataset.type === 'contentlet') {
            const parentReact = this.el.nativeElement.getBoundingClientRect();
            const targetRect = target.getBoundingClientRect();
            const mouseY = event.clientY;
            const isTop = mouseY < targetRect.top + targetRect.height / 2;

            this.pointerPosition = {
                left: `${targetRect.left - parentReact.left}px`,
                width: `${targetRect.width}px`,
                opacity: '1',
                top: isTop
                    ? `${targetRect.top - parentReact.top}px`
                    : `${targetRect.top - parentReact.top + targetRect.height}px`
            };
        }

        event.stopPropagation();
        event.preventDefault();
    }
}
