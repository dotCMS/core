import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostBinding,
    HostListener,
    inject,
    output,
    signal
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

    readonly $isActive = signal(false);

    readonly #store = inject(DotContentDriveStore);

    /**
     * @description Get the active state of the dropzone
     * @returns {boolean} - The active state of the dropzone
     */
    @HostBinding('class.active') get active(): boolean {
        return this.$isActive();
    }

    /**
     * @description Set the dropzone as active when the drag enters the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent & { fromElement: HTMLElement }) {
        event.stopPropagation();
        event.preventDefault();

        if (event.fromElement) {
            return;
        }

        this.$isActive.set(true);

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
        this.$isActive.set(false);
    }

    /**
     * @description Set the dropzone as inactive when the drag ends
     * @param event - DragEvent
     */
    @HostListener('dragend', ['$event'])
    onDragEnd(event: DragEvent) {
        event.preventDefault();
        this.$isActive.set(false);
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

        this.$isActive.set(false);

        if (files?.length) {
            this.uploadFiles.emit({ files });
        }
    }
}
