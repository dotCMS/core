import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Internal renderer for the icon fallback. Renders a Material Symbols glyph
 * whose size auto-scales with the thumbnail container (cqmin units — the
 * viewer host declares `container-type: size`) unless an explicit `size`
 * override is provided. Color is themeable from the outside via the
 * `--dot-content-thumbnail-icon-color` CSS custom property.
 */
@Component({
    selector: 'dot-content-thumbnail-icon',
    template: `
        <i
            [style.font-size]="$size()"
            [attr.aria-label]="$alt()"
            class="material-symbols-outlined text-[clamp(16px,55cqmin,72px)] leading-none text-(--dot-content-thumbnail-icon-color,var(--gray-700)) select-none"
            role="img"
            data-testId="dot-content-thumbnail-icon">
            {{ $icon() }}
        </i>
    `,
    host: { class: 'flex h-full w-full items-center justify-center' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailIconComponent {
    $icon = input.required<string>({ alias: 'icon' });
    $alt = input<string>('', { alias: 'alt' });
    /** Explicit font-size override (e.g. '48px'); when unset the glyph auto-scales via container queries. */
    $size = input<string | undefined>(undefined, { alias: 'size' });
}
