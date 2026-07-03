import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Internal renderer for video thumbnails. Playable videos render with native
 * controls; non-playable ones show a static first frame — the `src` carries a
 * `#t=0.1` media fragment (set by the mapper) so the browser seeks and paints
 * that frame without playback (replaces the legacy Stencil canvas approach).
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
                class="size-full"
                controls
                data-testId="dot-content-thumbnail-video"></video>
        } @else {
            <video
                (loadeddata)="loaded.emit()"
                (error)="failed.emit()"
                [src]="$src()"
                [attr.aria-label]="$alt()"
                class="pointer-events-none size-full object-cover"
                preload="metadata"
                muted
                playsinline
                disablepictureinpicture
                tabindex="-1"
                data-testId="dot-content-thumbnail-video-frame"></video>
        }
    `,
    host: { class: 'block h-full w-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailVideoComponent {
    $src = input.required<string>({ alias: 'src' });
    $alt = input<string>('', { alias: 'alt' });
    $playable = input<boolean>(false, { alias: 'playable' });

    loaded = output<void>();
    failed = output<void>();
}
