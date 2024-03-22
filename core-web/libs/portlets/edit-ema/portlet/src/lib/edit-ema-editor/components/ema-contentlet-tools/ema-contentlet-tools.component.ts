import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';

import { ActionPayload } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

const BUTTON_WIDTH = 40;
const BUTTON_HEIGHT = 40;
const ACTIONS_CONTAINER_WIDTH = 128;
const ACTIONS_CONTAINER_HEIGHT = 40;

@Component({
    selector: 'dot-ema-contentlet-tools',
    standalone: true,
    imports: [CommonModule, ButtonModule, MenuModule],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent {
    @ViewChild('dragImage') dragImage: ElementRef;
    private dotMessageService = inject(DotMessageService);

    private buttonPosition: 'after' | 'before' = 'after';

    @Input() contentlet: ContentletArea;
    @HostBinding('class.hide') @Input() hide = false;
    @Output() addContent = new EventEmitter<ActionPayload>();
    @Output() addForm = new EventEmitter<ActionPayload>();
    @Output() addWidget = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() delete = new EventEmitter<ActionPayload>();

    @Output() moveStart = new EventEmitter<ActionPayload>();
    @Output() moveStop = new EventEmitter<DragEvent>();

    items: MenuItem[] = [
        {
            label: this.dotMessageService.get('content'),
            command: () => {
                this.addContent.emit({
                    ...this.contentlet.payload,
                    position: this.buttonPosition
                });
            }
        },
        {
            label: this.dotMessageService.get('Widget'),
            command: () => {
                this.addWidget.emit({
                    ...this.contentlet.payload,
                    position: this.buttonPosition
                });
            }
        },
        {
            label: this.dotMessageService.get('form'),
            command: () => {
                this.addForm.emit({
                    ...this.contentlet.payload,
                    position: this.buttonPosition
                });
            }
        }
    ];

    dragStart(event: DragEvent, payload: ActionPayload): void {
        event.dataTransfer.setDragImage(this.dragImage.nativeElement, 0, 0);

        this.moveStart.emit(payload);
    }

    dragEnd(event: DragEvent): void {
        this.moveStop.emit(event);
    }

    /**
     * Set the position flag to add the contentlet before or after the current one
     *
     * @param {('before' | 'after')} position
     * @memberof EmaContentletToolsComponent
     */
    setPositionFlag(position: 'before' | 'after'): void {
        this.buttonPosition = position;
    }

    /**
     * Set the position for the bounds div
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    getPosition(): Record<string, string> {
        return {
            left: `${this.contentlet.x}px`,
            top: `${this.contentlet.y}px`,
            width: `${this.contentlet.width}px`,
            height: `${this.contentlet.height}px`
        };
    }

    /**
     * Set the position for the top add button
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    getTopButtonPosition(): Record<string, string> {
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = this.contentlet.y - BUTTON_HEIGHT / 2;

        return {
            position: 'absolute',
            left: this.contentlet.width < 250 ? `${this.contentlet.x + 8}px` : `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }

    /**
     * Set the position for the bottom add button
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    getBottomButtonPosition(): Record<string, string> {
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = this.contentlet.y + this.contentlet.height - BUTTON_HEIGHT / 2;

        return {
            position: 'absolute',
            top: `${buttonTop}px`,
            left: `${buttonLeft}px`,
            zIndex: '1'
        };
    }

    /**
     * Set the position for the action buttons
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    getActionPosition(): Record<string, string> {
        const contentletCenterX = this.contentlet.x + this.contentlet.width;
        const left = contentletCenterX - ACTIONS_CONTAINER_WIDTH - 8;
        const top = this.contentlet.y - ACTIONS_CONTAINER_HEIGHT / 2;

        return {
            position: 'absolute',
            left: `${left}px`,
            top: `${top}px`,
            zIndex: '1'
        };
    }

    /**
     *
     * Checks if the container is empty, based on the identifier
     * @readonly
     * @type {boolean}
     * @memberof EmaContentletToolsComponent
     */
    get isContainerEmpty(): boolean {
        return this.contentlet.payload.contentlet.identifier === 'TEMP_EMPTY_CONTENTLET';
    }
}
