import {
    booleanAttribute,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostBinding,
    HostListener,
    inject,
    input,
    output,
    signal
} from '@angular/core';

import { TreeNodeData } from '@dotcms/dotcms-models';

import { DROPZONE_STATE } from './constants';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DOT_DRAG_ITEM } from '../dot-folder-list-view/constants';
import { DotUploadFiles } from '../dot-upload-type-selector/models';

/**
 * Drop target for uploading files by dragging them from the OS. Wraps whatever content it is given
 * and paints a full-surface overlay while a file drag is over it. Shared by Content Drive and the
 * AssetPicker.
 *
 * Presentational: the host owns the target folder and what an upload means.
 */
@Component({
    selector: 'dot-upload-dropzone',
    imports: [DotMessagePipe],
    templateUrl: './dot-upload-dropzone.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'relative h-full w-full max-h-full min-h-0' },
    styles: `
        :host.active::after,
        :host.active .message {
            opacity: 1;
            visibility: visible;
        }

        :host::after {
            content: '';
            position: absolute;
            inset: 0;
            z-index: 1000;
            border: 1px dashed var(--color-palette-primary-500);
            border-radius: 1rem;
            background-color: rgba(246, 249, 252, 0.8);
            opacity: 0;
            visibility: visible;
            transition: all 0.3s ease-in-out;
            pointer-events: none;
        }
    `
})
export class DotUploadDropzoneComponent {
    /**
     * Folder the dropped files land in. Carried straight into {@link uploadFiles}.
     * @type {TreeNodeData | undefined}
     * @alias targetFolder
     */
    readonly $targetFolder = input<TreeNodeData | undefined>(undefined, { alias: 'targetFolder' });

    /**
     * Refuses the upload, without hiding the zone.
     *
     * Set where the user cannot add content to the target folder. The host owns why: in Content
     * Drive it is the same CAN_ADD_CHILDREN gate the New menu and the folder tree use, so one route
     * into a folder cannot quietly allow what the others forbid.
     *
     * The overlay still appears on drag, carrying {@link $disabledMessage} instead of the usual
     * prompt: a zone that goes quiet reads as a broken drop target, while one that explains itself
     * tells the user what to ask an administrator for.
     *
     * @type {boolean}
     * @alias disabled
     */
    readonly $disabled = input<boolean, unknown>(false, {
        alias: 'disabled',
        transform: booleanAttribute
    });

    /**
     * Shown in the overlay in place of the usual prompt while {@link $disabled} is set. Already
     * translated by the host, which owns the wording for why its own uploads are refused.
     *
     * Empty falls back to the default prompt, so a host that disables without explaining still
     * renders something coherent.
     *
     * @type {string}
     * @alias disabledMessage
     */
    readonly $disabledMessage = input<string>('', { alias: 'disabledMessage' });

    /** Emitted once files are dropped on the zone. */
    readonly uploadFiles = output<DotUploadFiles>();

    /**
     * Emitted when an external file drag enters the zone. Content Drive uses it to dismiss its
     * context menu; hosts without one can ignore it.
     */
    readonly dragEnter = output<void>();

    readonly elementRef = inject(ElementRef);

    readonly state = signal<string>(DROPZONE_STATE.INACTIVE);

    /**
     * @description Get the active state of the dropzone
     * @returns {boolean} - The active state of the dropzone
     */
    @HostBinding('class.active') get active(): boolean {
        return this.state() === DROPZONE_STATE.ACTIVE;
    }

    /**
     * @description Set the dropzone as internal drag
     */
    @HostListener('window:dragstart')
    onWindowDragStart() {
        this.state.set(DROPZONE_STATE.INTERNAL_DRAG);
    }

    /**
     * @description Set the dropzone as not internal drag
     */
    @HostListener('window:dragend')
    @HostListener('window:drop')
    onWindowDragEnd() {
        this.state.set(DROPZONE_STATE.INACTIVE);
    }

    /**
     * @description Set the dropzone as active when the drag enters the dropzone
     * @param event - DragEvent
     */
    @HostListener('dragenter', ['$event'])
    onDragEnter(event: DragEvent) {
        event.stopPropagation();
        event.preventDefault();

        // Dragging rows around inside the host is not an upload. `$disabled` is deliberately absent
        // here: the overlay is how the refusal gets explained, so it still has to open.
        if (
            this.state() === DROPZONE_STATE.INTERNAL_DRAG ||
            event.dataTransfer?.types.includes(DOT_DRAG_ITEM)
        ) {
            return;
        }

        this.state.set(DROPZONE_STATE.ACTIVE);
        this.dragEnter.emit();
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
        this.state.set(DROPZONE_STATE.INACTIVE);
    }

    /**
     * @description Set the dropzone as inactive when the drag ends
     * @param event - DragEvent
     */
    @HostListener('dragend', ['$event'])
    onDragEnd(event: DragEvent) {
        event.preventDefault();
        this.state.set(DROPZONE_STATE.INACTIVE);
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

        this.state.set(DROPZONE_STATE.INACTIVE);

        if (!this.$disabled() && files?.length) {
            this.uploadFiles.emit({ files, targetFolder: this.$targetFolder() });
        }
    }
}
