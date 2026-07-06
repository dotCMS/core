import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Internal renderer for raster/vector image thumbnails (`image`, `pdf`, `svg`
 * types). Purely presentational — visibility during load is orchestrated by
 * the viewer, which hides this renderer until it emits `loaded`. Kept private
 * to the `dot-content-thumbnail` folder — consumers use the viewer.
 *
 * Styles are self-contained (no Tailwind): the component also ships inside
 * bundles without the Tailwind utilities (e.g. dotcms-binary-field-builder).
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
            class="thumbnail-image"
            data-testId="dot-content-thumbnail-image" />
    `,
    styles: `
        :host {
            display: block;
            height: 100%;
            width: 100%;
        }

        .thumbnail-image {
            height: 100%;
            width: 100%;
            object-fit: cover;
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailImageComponent {
    $src = input.required<string>({ alias: 'src' });
    $alt = input<string>('', { alias: 'alt' });

    loaded = output<void>();
    failed = output<void>();
}
