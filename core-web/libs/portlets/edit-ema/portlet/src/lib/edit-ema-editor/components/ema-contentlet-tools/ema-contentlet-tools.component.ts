import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChange,
    SimpleChanges,
    ViewChild,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';

import { ActionPayload, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

const BUTTON_WIDTH = 40;
const BUTTON_HEIGHT = 40;
const ACTIONS_CONTAINER_HEIGHT = 40;

@Component({
    selector: 'dot-ema-contentlet-tools',
    standalone: true,
    imports: [CommonModule, ButtonModule, MenuModule],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent implements OnInit, OnChanges {
    @ViewChild('menu') menu: Menu;
    @ViewChild('menuVTL') menuVTL: Menu;
    @ViewChild('dragImage') dragImage: ElementRef;
    private dotMessageService = inject(DotMessageService);

    private buttonPosition: 'after' | 'before' = 'after';

    @Input() contentletArea: ContentletArea;
    @HostBinding('class.hide') @Input() hide = false;
    @Output() addContent = new EventEmitter<ActionPayload>();
    @Output() addForm = new EventEmitter<ActionPayload>();
    @Output() addWidget = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() editVTL = new EventEmitter<VTLFile>();
    @Output() delete = new EventEmitter<ActionPayload>();

    items: MenuItem[] = [
        {
            label: this.dotMessageService.get('content'),
            command: () => {
                this.addContent.emit({
                    ...this.contentletArea.payload,
                    position: this.buttonPosition
                });
            }
        },
        {
            label: this.dotMessageService.get('Widget'),
            command: () => {
                this.addWidget.emit({
                    ...this.contentletArea.payload,
                    position: this.buttonPosition
                });
            }
        },
        {
            label: this.dotMessageService.get('form'),
            command: () => {
                this.addForm.emit({
                    ...this.contentletArea.payload,
                    position: this.buttonPosition
                });
            }
        }
    ];

    vtlFiles: MenuItem[] = [];

    ACTIONS_CONTAINER_WIDTH: number; // Now is dynamic based on the page type (Headless - VTL)

    ngOnInit() {
        this.setVtlFiles();
        this.ACTIONS_CONTAINER_WIDTH = this.contentletArea.payload.vtlFiles ? 178 : 128;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (!changes.contentletArea) {
            return;
        }

        if (this.hasContentletOrContainerChanged(changes.contentletArea)) {
            this.hideMenus();
        }
    }

    /**
     * Sets the VTL files for the component.
     *
     * @memberof EmaContentletToolsComponent
     */
    setVtlFiles() {
        this.vtlFiles = this.contentletArea.payload.vtlFiles?.map((file) => ({
            label: file.name,
            command: () => {
                this.editVTL.emit(file);
            }
        }));
    }

    dragStart(event: DragEvent): void {
        event.dataTransfer.setDragImage(this.dragImage.nativeElement, 0, 0);
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
            left: `${this.contentletArea.x}px`,
            top: `${this.contentletArea.y}px`,
            width: `${this.contentletArea.width}px`,
            height: `${this.contentletArea.height}px`
        };
    }

    /**
     * Set the position for the top add button
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    getTopButtonPosition(): Record<string, string> {
        const contentletCenterX = this.contentletArea.x + this.contentletArea.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = this.contentletArea.y - BUTTON_HEIGHT / 2;

        return {
            position: 'absolute',
            left:
                this.contentletArea.width < 250
                    ? `${this.contentletArea.x + 8}px`
                    : `${buttonLeft}px`,
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
        const contentletCenterX = this.contentletArea.x + this.contentletArea.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = this.contentletArea.y + this.contentletArea.height - BUTTON_HEIGHT / 2;

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
        const contentletCenterX = this.contentletArea.x + this.contentletArea.width;
        const left = contentletCenterX - this.ACTIONS_CONTAINER_WIDTH - 8;
        const top = this.contentletArea.y - ACTIONS_CONTAINER_HEIGHT / 2;

        return {
            position: 'absolute',
            left: `${left}px`,
            top: `${top}px`,
            zIndex: '1'
        };
    }

    /**
     * Check if Contentlet or Container are changed
     *
     * @private
     * @param {SimpleChange} contentletChange
     * @return {*}  {boolean}
     * @memberof EmaContentletToolsComponent
     */
    private hasContentletOrContainerChanged(contentletChange: SimpleChange): boolean {
        const currentValue = contentletChange.currentValue?.payload;
        const previousValue = contentletChange.previousValue?.payload;

        const hasContentletIdentifierChanged =
            currentValue?.contentlet.identifier !== previousValue?.contentlet.identifier;
        const hasUUIDChanged = currentValue?.container.uuid !== previousValue?.container.uuid;
        const hasContainerIdentifierChanged =
            currentValue?.container.identifier !== previousValue?.container.identifier;

        return hasContentletIdentifierChanged || hasUUIDChanged || hasContainerIdentifierChanged;
    }

    /**
     * Hide all context menus when the contentlet changes
     *
     * @memberof EmaContentletToolsComponent
     */
    hideMenus() {
        this.menu?.hide();
        this.menuVTL?.hide();
    }

    /**
     *
     * Checks if the container is empty, based on the identifier
     * @readonly
     * @type {boolean}
     * @memberof EmaContentletToolsComponent
     */
    get isContainerEmpty(): boolean {
        return this.contentletArea.payload.contentlet.identifier === 'TEMP_EMPTY_CONTENTLET';
    }
}
