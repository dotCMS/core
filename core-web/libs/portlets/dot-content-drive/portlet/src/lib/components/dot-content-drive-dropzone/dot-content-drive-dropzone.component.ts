import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostBinding,
    HostListener,
    inject,
    output
} from '@angular/core';

import { DotContentDriveUploadFiles } from '@dotcms/portlets/content-drive/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

@Component({
    selector: 'dot-content-drive-dropzone',
    imports: [DotMessagePipe],
    templateUrl: './dot-content-drive-dropzone.component.html',
    styleUrl: './dot-content-drive-dropzone.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentDriveDropzoneComponent {
    readonly uploadFiles = output<DotContentDriveUploadFiles>();

    readonly elementRef = inject(ElementRef);

    readonly state = signalState<{ isInternalDrag: boolean; isActive: boolean }>({
        isInternalDrag: false,
        isActive: false
    });

    readonly #store = inject(DotContentDriveStore);

    /**
     * @description Get the active state of the dropzone
     * @returns {boolean} - The active state of the dropzone
     */
    @HostBinding('class.active') get active(): boolean {
        return this.state.isActive();
    }

    /**
     * @description Set the dropzone as internal drag
     * @memberof DotContentDriveDropzoneComponent
     */
    @HostListener('window:dragstart')
    onWindowDragStart() {
        patchState(this.state, { isInternalDrag: true });
    }

    /**
     * @description Set the dropzone as not internal drag
     * @memberof DotContentDriveDropzoneComponent
     */
    @HostListener('window:dragend')
    @HostListener('window:drop')
    onWindowDragEnd() {
        patchState(this.state, { isInternalDrag: false });
    }

    /**
     * @description Set the dropzone as active when the drag enters the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        if (this.state.isInternalDrag()) {
            return;
        }

        patchState(this.state, { isActive: true });

        // Reset the context menu
        this.#store.resetContextMenu();
    }

    /**
     * @description Prevent the default behavior to allow drop and not opening the file in the browser
     * @param event - DragEvent
     */
    @HostListener('dragover', ['$event'])
    onDragOver(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();
    }

    /**
     * @description Set the dropzone as inactive when the drag leaves the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragleave', ['$event'])
    onDragLeave(event: DragEvent) {
        event.preventDefault();

        // Check if the relatedTarget (where the drag is going) is still within the dropzone
        const relatedTarget = event.relatedTarget as Node;

        if (relatedTarget && this.elementRef.nativeElement.contains(relatedTarget)) {
            return; // Still within the dropzone, don't deactivate
        }

        // Drag has left the dropzone
        patchState(this.state, { isActive: false });
    }

    /**
     * @description Set the dropzone as inactive when the drag ends
     * @param event - DragEvent
     */
    @HostListener('dragend', ['$event'])
    onDragEnd(event: DragEvent) {
        event.preventDefault();
        patchState(this.state, { isActive: false });
    }

    /**
     * @description Set the dropzone as inactive when the drag ends on the dropzone
     * @param event - DragEvent
     */

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        const files = event.dataTransfer?.files ?? undefined;

        patchState(this.state, { isActive: false });

        if (files?.length) {
            this.uploadFiles.emit({ files, targetFolderId: this.#store.selectedNode()?.data.id });
        }
    }
}
