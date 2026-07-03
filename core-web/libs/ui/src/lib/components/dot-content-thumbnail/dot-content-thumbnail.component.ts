import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    input,
    linkedSignal,
    output
} from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentThumbnailIconComponent } from './components/dot-content-thumbnail-icon.component';
import { DotContentThumbnailImageComponent } from './components/dot-content-thumbnail-image.component';
import { DotContentThumbnailVideoComponent } from './components/dot-content-thumbnail-video.component';
import {
    DotContentThumbnail,
    DotContentThumbnailState,
    DotContentThumbnailType,
    DotContentletThumbnailOptions
} from './models/dot-content-thumbnail.model';
import { contentletToThumbnailModel } from './utils/dot-content-thumbnail.utils';

const EMPTY_THUMBNAIL: DotContentThumbnail = {
    type: 'icon',
    src: '',
    icon: 'insert_drive_file',
    alt: ''
};

/**
 * Presentational thumbnail viewer for dotCMS content (saved contentlets and
 * temp files). Renders per-type (image/svg/pdf/video/icon) through internal
 * renderers, with a loading pulse and an icon fallback on media error.
 *
 * Two ways to feed it — pass exactly one:
 *
 * ```html
 * <!-- Common case: hand it the contentlet, it resolves the model itself -->
 * <dot-content-thumbnail [contentlet]="item" />
 * <dot-content-thumbnail [contentlet]="item" [options]="{ fieldVariable: 'asset' }" />
 *
 * <!-- Advanced: pass an already-resolved model (e.g. temp files) -->
 * <dot-content-thumbnail [thumbnail]="model" />
 * ```
 *
 * Fills its parent container (`h-full w-full`); media renders `object-cover`
 * (`object-contain` for SVG). The icon glyph auto-scales with the container
 * (override with `iconSize`) and can be recolored via the
 * `--dot-content-thumbnail-icon-color` CSS custom property.
 *
 * Replaces the deprecated Stencil `dot-contentlet-thumbnail` web component
 * and the former `dot-temp-file-thumbnail`.
 */
@Component({
    selector: 'dot-content-thumbnail',
    imports: [
        DotContentThumbnailImageComponent,
        DotContentThumbnailVideoComponent,
        DotContentThumbnailIconComponent
    ],
    template: `
        @let model = $model();
        @switch ($type()) {
            @case ('video') {
                <!-- Playable videos are never masked: the native element brings its own
                     controls and loading UX, and media loads can be slow/queued -->
                <dot-content-thumbnail-video
                    (loaded)="onLoaded()"
                    (failed)="onFailed()"
                    [src]="model.src"
                    [alt]="model.alt"
                    [playable]="model.playable ?? false"
                    [class.opacity-0]="$state() === 'loading' && !model.playable"
                    class="transition-opacity duration-300" />
            }
            @case ('icon') {
                <dot-content-thumbnail-icon
                    [icon]="model.icon || 'insert_drive_file'"
                    [alt]="model.alt"
                    [size]="$iconSize()" />
            }
            @default {
                <dot-content-thumbnail-image
                    (loaded)="onLoaded()"
                    (failed)="onFailed()"
                    [src]="model.src"
                    [alt]="model.alt"
                    [fit]="$type() === 'svg' ? 'contain' : 'cover'"
                    [class.opacity-0]="$state() === 'loading'"
                    class="transition-opacity duration-300" />
            }
        }

        @if ($hasLoadingOverlay()) {
            <div
                class="dot-content-thumbnail__loading pointer-events-none absolute inset-0 overflow-hidden bg-gray-200 transition-opacity duration-300"
                [class.opacity-0]="$state() !== 'loading'"
                data-testId="dot-content-thumbnail-loading"></div>
        }
    `,
    host: {
        class: 'relative flex h-full w-full items-center justify-center overflow-hidden [container-type:size]'
    },
    styles: `
        /* Sliding-highlight shimmer, exact replica of PrimeNG p-skeleton (needs
           keyframes, not expressible in Tailwind utilities). Uses the PrimeNG
           token when the theme provides it, with the Lara default as fallback. */
        .dot-content-thumbnail__loading::after {
            content: '';
            position: absolute;
            inset: 0;
            transform: translateX(-100%);
            background: linear-gradient(
                90deg,
                rgba(255, 255, 255, 0),
                var(--p-skeleton-animation-background, rgba(255, 255, 255, 0.4)),
                rgba(255, 255, 255, 0)
            );
            animation: dot-content-thumbnail-shimmer 1.2s infinite;
        }

        /* Stop compositing work once the overlay has faded out */
        .dot-content-thumbnail__loading.opacity-0::after {
            animation: none;
        }

        @keyframes dot-content-thumbnail-shimmer {
            to {
                transform: translateX(100%);
            }
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailComponent {
    /** Contentlet to preview; the component resolves the thumbnail model itself. */
    $contentlet = input<DotCMSContentlet | undefined>(undefined, { alias: 'contentlet' });
    /** Mapping options used with `contentlet` (fieldVariable, playableVideo…). */
    $options = input<DotContentletThumbnailOptions | undefined>(undefined, { alias: 'options' });
    /** Already-resolved model; takes precedence over `contentlet`. */
    $thumbnail = input<DotContentThumbnail | undefined>(undefined, { alias: 'thumbnail' });
    /** Explicit icon font-size override; when unset the glyph auto-scales with the container. */
    $iconSize = input<string | undefined>(undefined, { alias: 'iconSize' });

    stateChange = output<DotContentThumbnailState>();

    protected readonly $model = computed<DotContentThumbnail>(() => {
        const thumbnail = this.$thumbnail();

        if (thumbnail) {
            return thumbnail;
        }

        const contentlet = this.$contentlet();

        return contentlet
            ? contentletToThumbnailModel(contentlet, this.$options())
            : EMPTY_THUMBNAIL;
    });

    /** Effective render type; falls back to 'icon' on media error, resets when the model changes. */
    protected readonly $type = linkedSignal<DotContentThumbnailType>(() => this.$model().type);

    /** Skeleton applies to async media that stays hidden until it paints — not icons nor playable videos. */
    protected readonly $hasLoadingOverlay = computed(() => {
        const type = this.$type();

        return type !== 'icon' && !(type === 'video' && this.$model().playable);
    });

    /** Icon has no async load, so it starts (and resets) as 'loaded'. */
    protected readonly $state = linkedSignal<DotContentThumbnailState>(() =>
        this.$model().type === 'icon' ? 'loaded' : 'loading'
    );

    constructor() {
        effect(() => this.stateChange.emit(this.$state()));
    }

    protected onLoaded(): void {
        this.$state.set('loaded');
    }

    protected onFailed(): void {
        this.$state.set('error');
        this.$type.set('icon');
    }
}
