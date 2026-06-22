import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectButtonModule } from 'primeng/selectbutton';
import { SliderModule, SliderSlideEndEvent } from 'primeng/slider';

import { DotMessagePipe } from '@dotcms/ui';

import { BYTES_PER_KB } from '../../../image-editor.constants';
import { CompressionMode, CompressionOption } from '../../../models/image-editor.models';
import { imageEditorFileInfoEvents } from '../../../store/image-editor.events';
import { ImageEditorStore } from '../../../store/image-editor.store';

/**
 * File info / compression panel. Lets the user pick a compression strategy and,
 * when compression is active, tune its quality, while surfacing the current
 * preview file size. Binds to the {@link ImageEditorStore} `fileInfo` slice and
 * dispatches the matching {@link imageEditorFileInfoEvents} on user input; the
 * quality slider dispatches its committed value on `onSlideEnd`.
 */
@Component({
    selector: 'dot-image-editor-fileinfo-panel',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, SelectButtonModule, SliderModule, DotMessagePipe],
    templateUrl: './dot-image-editor-fileinfo-panel.component.html',
    styleUrl: './dot-image-editor-fileinfo-panel.component.scss'
})
export class DotImageEditorFileInfoPanelComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Panel event dispatcher for compression and quality changes. */
    protected readonly dispatch = injectDispatch(imageEditorFileInfoEvents);

    /** Selectable compression strategies. */
    protected readonly compressionOptions: CompressionOption[] = [
        { label: 'edit.content.image-editor.fileinfo.compression.none', value: 'none' },
        { label: 'edit.content.image-editor.fileinfo.compression.auto', value: 'auto' },
        { label: 'edit.content.image-editor.fileinfo.compression.jpeg', value: 'jpeg' },
        { label: 'edit.content.image-editor.fileinfo.compression.webp', value: 'webp' }
    ];

    /** Whether a compression strategy is active (so quality applies). */
    protected readonly isCompressing = computed(() => this.store.fileInfo().compression !== 'none');

    /** Human-readable current preview size, or an em dash when unknown. */
    protected readonly fileSize = computed(() => formatBytes(this.store.fileInfo().currentBytes));

    /** Original (source) image dimensions, or an em dash before the asset loads. */
    protected readonly originalSize = computed(() => {
        const { naturalWidth, naturalHeight } = this.store.assetContext();

        return naturalWidth && naturalHeight ? `${naturalWidth} × ${naturalHeight} px` : '—';
    });

    /** Dispatches the selected compression strategy. */
    protected compressionChanged(value: CompressionMode): void {
        this.dispatch.compressionChanged(value);
    }

    /** Dispatches the final quality value once the slider drag ends. */
    protected qualityChanged(event: SliderSlideEndEvent): void {
        this.dispatch.qualityChanged(event.value ?? 0);
    }
}

/** Formats a byte count to KB/MB with one decimal, or an em dash when null. */
function formatBytes(bytes: number | null): string {
    if (bytes == null) {
        return '—';
    }

    const kb = bytes / BYTES_PER_KB;
    if (kb < BYTES_PER_KB) {
        return `${kb.toFixed(1)} KB`;
    }

    return `${(kb / BYTES_PER_KB).toFixed(1)} MB`;
}
