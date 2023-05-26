import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    HostListener,
    Input
} from '@angular/core';

import { boxIcon, rowIcon } from '../../assets/icons';

type WidgetType = 'box' | 'row';

const iconsMap = {
    row: rowIcon,
    box: boxIcon
};

@Component({
    selector: 'dotcms-drag-box',
    templateUrl: './drag-box.component.html',
    styleUrls: ['./drag-box.component.scss'],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DragBoxComponent {
    public isDragging = false;

    @Input() label = '';
    @Input() type: WidgetType = 'box';

    get icon(): string {
        return iconsMap[this.type];
    }

    /**
     * @description
     * This is a workaround to avoid the drag image to be the default one.
     * It's important to do it on the document:mouseup because the gragend event is not
     * propagated to the host element when it's rendered by gridstack
     *
     * @memberof DragBoxComponent
     */
    @HostListener('document:mouseup') onMouseUpDocument(): void {
        if (this.isDragging) {
            this.setisDraggingState(false);
        }
    }

    @HostListener('dragend') onDragEndDocument(): void {
        if (this.isDragging) {
            this.setisDraggingState(false);
        }
    }

    /**
     * @description
     * Handle the mouse down event on the host element to set the dragging state
     *
     * @memberof DragBoxComponent
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
