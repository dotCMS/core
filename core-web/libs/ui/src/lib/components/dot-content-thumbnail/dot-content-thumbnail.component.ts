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
                    [class.thumbnail-media--hidden]="$state() === 'loading' && !model.playable"
                    class="thumbnail-media" />
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
                    [class.thumbnail-media--hidden]="$state() === 'loading'"
                    class="thumbnail-media" />
            }
        }

        @if ($hasLoadingOverlay()) {
            <div
                class="thumbnail-loading"
                [class.thumbnail-loading--hidden]="$state() !== 'loading'"
                data-testId="dot-content-thumbnail-loading"></div>
        }
    `,
    /* Styles are self-contained (no Tailwind): the component also ships inside
       bundles without the Tailwind utilities (e.g. dotcms-binary-field-builder). */
    styles: `
        :host {
            position: relative;
            display: flex;
            height: 100%;
            width: 100%;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            /* inline-size only: size containment would zero the content's height
               contribution and collapse auto-height wrappers (e.g. the file field
               below its 500px container breakpoint) */
            container-type: inline-size;
        }

        .thumbnail-media {
            transition: opacity 0.3s;
        }

        .thumbnail-media--hidden {
            opacity: 0;
        }

        .thumbnail-loading {
            position: absolute;
            inset: 0;
            overflow: hidden;
            pointer-events: none;
            background: var(--color-palette-gray-200, #e5e7eb);
            transition: opacity 0.3s;
        }

        .thumbnail-loading--hidden {
            opacity: 0;
        }

        /* Sliding-highlight shimmer, exact replica of PrimeNG p-skeleton. Uses the
           PrimeNG token when the theme provides it, with the Lara default as fallback. */
        .thumbnail-loading::after {
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
        .thumbnail-loading--hidden::after {
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
