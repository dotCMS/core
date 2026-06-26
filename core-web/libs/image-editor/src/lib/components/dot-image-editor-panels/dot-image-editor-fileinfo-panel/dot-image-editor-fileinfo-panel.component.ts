import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';
import { SliderModule, SliderSlideEndEvent } from 'primeng/slider';

import { map } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { BYTES_PER_KB, LIBVIPS_CONFIG_KEY } from '../../../image-editor.constants';
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
    imports: [FormsModule, SelectModule, SliderModule, DotMessagePipe],
    templateUrl: './dot-image-editor-fileinfo-panel.component.html',
    styleUrl: './dot-image-editor-fileinfo-panel.component.scss'
})
export class DotImageEditorFileInfoPanelComponent {
    /** Image editor state store, provided by the owning dialog component. */
    protected readonly store = inject(ImageEditorStore);

    /** Panel event dispatcher for compression and quality changes. */
    protected readonly dispatch = injectDispatch(imageEditorFileInfoEvents);

    readonly #propertiesService = inject(DotPropertiesService);

    /**
     * Whether the libvips image engine is enabled server-side. AVIF is a libvips-only
     * output format (the legacy engine can't render it), so the AVIF option is hidden
     * unless this is on. Off until the server replies.
     */
    readonly #libvipsEnabled = toSignal(
        this.#propertiesService
            .getKey(LIBVIPS_CONFIG_KEY)
            .pipe(map((value) => value === true || value === 'true')),
        { initialValue: false }
    );

    /** All compression strategies; AVIF is libvips-only and filtered when disabled. */
    readonly #allCompressionOptions: CompressionOption[] = [
        { label: 'edit.content.image-editor.fileinfo.compression.none', value: 'none' },
        { label: 'edit.content.image-editor.fileinfo.compression.auto', value: 'auto' },
        { label: 'edit.content.image-editor.fileinfo.compression.jpeg', value: 'jpeg' },
        { label: 'edit.content.image-editor.fileinfo.compression.webp', value: 'webp' },
        { label: 'edit.content.image-editor.fileinfo.compression.avif', value: 'avif' }
    ];

    /** Selectable compression strategies (AVIF shown only when libvips is enabled). */
    protected readonly compressionOptions = computed<CompressionOption[]>(() =>
        this.#libvipsEnabled()
            ? this.#allCompressionOptions
            : this.#allCompressionOptions.filter((option) => option.value !== 'avif')
    );

    /** Whether a compression strategy is active (so quality applies). */
    protected readonly isCompressing = computed(() => this.store.fileInfo().compression !== 'none');

    /** Human-readable current preview size, or an em dash when unknown. */
    protected readonly fileSize = computed(() => formatBytes(this.store.fileInfo().currentBytes));

    /** Original (source) image dimensions, or an em dash before the asset loads. */
    protected readonly originalSize = computed(() => {
        const { naturalWidth, naturalHeight } = this.store.assetContext();

        return naturalWidth && naturalHeight ? `${naturalWidth} × ${naturalHeight} px` : '—';
    });

    /**
     * The effective output format of the saved image: the chosen compression format,
     * or the source format when compression doesn't change it (none/auto).
     */
    protected readonly format = computed(() => {
        switch (this.store.fileInfo().compression) {
            case 'jpeg':
                return 'JPEG';
            case 'webp':
                return 'WEBP';
            case 'avif':
                return 'AVIF';
            default:
                return sourceFormat(this.store.assetContext().mimeType) ?? '—';
        }
    });

    /**
     * Current focal point as normalized `x, y` (0..1). Not yet read back from the
     * asset, so it shows the store value — defaulting to the centre (0.50, 0.50).
     */
    protected readonly focalPoint = computed(() => {
        const { x, y } = this.store.focalPoint();

        return `${x.toFixed(2)}, ${y.toFixed(2)}`;
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

/** Derives a display format label from a MIME type (`image/png` -> `PNG`), or null. */
function sourceFormat(mimeType: string): string | null {
    // Strip the `image/` type and any structured suffix (`svg+xml` -> `svg`).
    const subtype = mimeType?.split('/')[1]?.split('+')[0];

    return subtype ? subtype.toUpperCase() : null;
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
