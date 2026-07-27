import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Internal renderer for video thumbnails. Playable videos render with native
 * controls; non-playable ones show a static first frame — the `src` carries a
 * `#t=0.1` media fragment (set by the mapper) so the browser seeks and paints
 * that frame without playback (replaces the legacy Stencil canvas approach).
 *
 * Styles are self-contained (no Tailwind): the component also ships inside
 * bundles without the Tailwind utilities (e.g. dotcms-binary-field-builder).
 */
@Component({
    selector: 'dot-content-thumbnail-video',
    template: `
        @if ($playable()) {
            <!-- loadedmetadata (not loadeddata): the element is visible from the start,
                 so 'loaded' only needs to mean "the media resolved", not "frame painted" -->
            <video
                (loadedmetadata)="loaded.emit()"
                (error)="failed.emit()"
                [src]="$src()"
                [attr.aria-label]="$alt()"
                class="thumbnail-video"
                controls
                data-testId="dot-content-thumbnail-video"></video>
        } @else {
            <video
                (loadeddata)="loaded.emit()"
                (error)="failed.emit()"
                [src]="$src()"
                [attr.aria-label]="$alt()"
                class="thumbnail-video thumbnail-video--frame"
                preload="metadata"
                muted
                playsinline
                disablepictureinpicture
                tabindex="-1"
                data-testId="dot-content-thumbnail-video-frame"></video>
        }
    `,
    styles: `
        :host {
            display: block;
            height: 100%;
            width: 100%;
        }

        .thumbnail-video {
            height: 100%;
            width: 100%;
        }

        .thumbnail-video--frame {
            object-fit: cover;
            pointer-events: none;
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailVideoComponent {
    $src = input.required<string>({ alias: 'src' });
    $alt = input<string>('', { alias: 'alt' });
    $playable = input<boolean>(false, { alias: 'playable' });

    loaded = output<void>();
    failed = output<void>();
}
