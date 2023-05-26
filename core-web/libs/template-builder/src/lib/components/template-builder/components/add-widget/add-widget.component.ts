import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostListener,
    Input
} from '@angular/core';

import { colIcon, rowIcon } from './icons';

import { WidgetType } from '../../models/models';

const iconsMap = {
    row: rowIcon,
    col: colIcon
};

@Component({
    selector: 'dotcms-add-widget',
    templateUrl: './add-widget.component.html',
    styleUrls: ['./add-widget.component.scss'],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddWidgetComponent {
    public isDragging = false;

    @Input() label = 'Add Widget';
    @Input() type: WidgetType = 'row';

    get icon(): string {
        return iconsMap[this.type];
    }

    /**
     * @description
     * This is a workaround to avoid the drag image to be the default one.
     * It's important to do it on the document:mouseup because the gragend event is not
     * propagated to the host element when it's rendered by gridstack
     *
     * @memberof AddWidgetComponent
     */
    @HostListener('dragend')
    @HostListener('document:mouseup')
    onDragEnd(): void {
        if (this.isDragging) {
            this.setisDraggingState(false);
        }
    }

    onDragEndDocument(): void {
        if (this.isDragging) {
            this.setisDraggingState(false);
        }
    }

    /**
     * @description
     * Handle the mouse down event on the host element to set the dragging state
     *
     * @memberof AddWidgetComponent
     */
    @HostListener('mousedown', ['$event']) onMouseDown(): void {
        this.setisDraggingState(true);
    }

    constructor(private readonly cd: ChangeDetectorRef) {}

    private setisDraggingState(isDragging: boolean): void {
        this.isDragging = isDragging;
        this.cd.detectChanges();
    }
}
