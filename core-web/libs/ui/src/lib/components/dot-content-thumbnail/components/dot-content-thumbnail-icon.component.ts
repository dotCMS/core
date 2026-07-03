import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Internal renderer for the icon fallback. Renders a Material Symbols glyph
 * whose size auto-scales with the thumbnail container (cqmin units — the
 * viewer host declares `container-type: size`) unless an explicit `size`
 * override is provided. Color is themeable from the outside via the
 * `--dot-content-thumbnail-icon-color` CSS custom property.
 *
 * Styles are self-contained (no Tailwind): the component also ships inside
 * bundles without the Tailwind utilities (e.g. dotcms-binary-field-builder).
 */
@Component({
    selector: 'dot-content-thumbnail-icon',
    template: `
        <i
            [style.font-size]="$size()"
            [attr.aria-label]="$alt()"
            class="material-symbols-outlined"
            role="img"
            data-testId="dot-content-thumbnail-icon">
            {{ $icon() }}
        </i>
    `,
    styles: `
        :host {
            display: flex;
            height: 100%;
            width: 100%;
            align-items: center;
            justify-content: center;
        }

        i {
            font-size: clamp(16px, 55cqmin, 72px);
            line-height: 1;
            user-select: none;
            color: var(--dot-content-thumbnail-icon-color, var(--gray-700, #6c7389));
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailIconComponent {
    $icon = input.required<string>({ alias: 'icon' });
    $alt = input<string>('', { alias: 'alt' });
    /** Explicit font-size override (e.g. '48px'); when unset the glyph auto-scales via container queries. */
    $size = input<string | undefined>(undefined, { alias: 'size' });
}
