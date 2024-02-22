import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';

import { ActionPayload } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

@Component({
    selector: 'dot-ema-contentlet-tools',
    standalone: true,
    imports: [CommonModule, ButtonModule, MenuModule],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent {
    private dotMessageService = inject(DotMessageService);

    private buttonPosition: 'after' | 'before' = 'after';

    @Input() contentlet: ContentletArea;
    @Output() addContent = new EventEmitter<ActionPayload>();
    @Output() addForm = new EventEmitter<ActionPayload>();
    @Output() addWidget = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() delete = new EventEmitter<ActionPayload>();

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
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y - buttonHeight / 2;

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
        const buttonWidth = 40;
        const buttonHeight = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width / 2;
        const buttonLeft = contentletCenterX - buttonWidth / 2;
        const buttonTop = this.contentlet.y + this.contentlet.height - buttonHeight / 2;

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
        const width = 84;
        const height = 40;
        const contentletCenterX = this.contentlet.x + this.contentlet.width;
        const left = contentletCenterX - width - 8;
        const top = this.contentlet.y - height / 2;

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
