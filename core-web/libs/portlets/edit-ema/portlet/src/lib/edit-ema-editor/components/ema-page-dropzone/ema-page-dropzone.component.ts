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

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { ActionPayload, ContainerPayload } from '../../../shared/models';

export interface ContentletArea {
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
    contentlets: ContentletArea[];
    payload: ActionPayload | string;
}

interface Column {
    x: number;
    y: number;
    width: number;
    height: number;
    containers: Container[];
}

export interface EmaDragItem {
    baseType: string;
    contentType: string;
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
    @Input() item: EmaDragItem;
    @Output() place = new EventEmitter<ActionPayload>();

    private readonly dotMessageService: DotMessageService = inject(DotMessageService);

    pointerPosition: Record<string, string> = {
        left: '0',
        width: '0',
        opacity: '0',
        top: '0'
    };

    constructor(private readonly el: ElementRef) {}

    /**
     * Set the style for the item
     *
     * @param {(Row | Column | Container | ContentletArea)} item
     * @param {string} [border='black']
     * @return {*}  {Record<string, string>}
     * @memberof EmaPageDropzoneComponent
     */
    getPosition(item: Row | Column | Container | ContentletArea): Record<string, string> {
        return {
            position: 'absolute',
            left: `${item.x}px`,
            top: `${item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`
        };
    }

    /**
     * Emit place event and reset pointer position
     *
     * @param {DragEvent} event
     * @memberof EmaPageDropzoneComponent
     */
    onDrop(event: DragEvent): void {
        const target = event.target as HTMLDivElement;
        const data = JSON.parse(target.dataset.payload);
        const isTop = this.isTop(event);

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

    /**
     * Return error message if the contentlet can't be placed in the container
     * or empty string if it can be placed
     *
     * @param {Container} { payload }
     * @return {*}  {boolean}
     * @memberof EmaPageDropzoneComponent
     */
    getErrorMessage(paylaod: ActionPayload | string): string {
        const { container = {} } =
            typeof paylaod === 'string' ? JSON.parse(paylaod) : paylaod || {};
        const { acceptTypes = '', maxContentlets } = container;

        if (!this.isValidContentType(acceptTypes)) {
            return this.dotMessageService.get(
                'edit.ema.page.dropzone.invalid.contentlet.type',
                this.item.contentType
            );
        }

        if (!this.contentCanFitInContainer(container)) {
            const message =
                maxContentlets === 1
                    ? 'edit.ema.page.dropzone.one.max.contentlet'
                    : 'edit.ema.page.dropzone.max.contentlets';

            return this.dotMessageService.get(message, maxContentlets);
        }

        return '';
    }

    private isTop(event: DragEvent): boolean {
        const target = event.target as HTMLDivElement;
        const targetRect = target.getBoundingClientRect();
        const mouseY = event.clientY;

        return mouseY < targetRect.top + targetRect.height / 2;
    }

    private isValidContentType(acceptTypes: string) {
        if (this.item.baseType === DotCMSBaseTypesContentTypes.WIDGET) {
            return true;
        }

        const acceptTypesArr = acceptTypes.split(',');

        return acceptTypesArr.includes(this.item.contentType);
    }

    private contentCanFitInContainer({ contentletsId, maxContentlets }: ContainerPayload): boolean {
        const amountOfContentlets = contentletsId?.length || 0;

        return amountOfContentlets < maxContentlets;
    }
}
