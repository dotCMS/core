import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Internal renderer for raster/vector image thumbnails (`image`, `pdf`, `svg`
 * types). Purely presentational — visibility during load is orchestrated by
 * the viewer, which hides this renderer until it emits `loaded`. Kept private
 * to the `dot-content-thumbnail` folder — consumers use the viewer.
 */
@Component({
    selector: 'dot-content-thumbnail-image',
    template: `
        <img
            (load)="loaded.emit()"
            (error)="failed.emit()"
            [src]="$src()"
            [alt]="$alt()"
            [title]="$alt()"
            [class.object-cover]="$fit() === 'cover'"
            [class.object-contain]="$fit() === 'contain'"
            class="size-full"
            data-testId="dot-content-thumbnail-image" />
    `,
    host: { class: 'block h-full w-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailImageComponent {
    $src = input.required<string>({ alias: 'src' });
    $alt = input<string>('', { alias: 'alt' });
    $fit = input<'cover' | 'contain'>('cover', { alias: 'fit' });

    loaded = output<void>();
    failed = output<void>();
}
