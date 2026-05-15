import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const HEX_COLOR_PATTERN = /^#([0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i;

@Component({
    selector: 'dot-color-icon',
    standalone: true,
    template: `
        <ng-content />
    `,
    host: {
        class: 'rounded-xl flex items-center justify-center shrink-0',
        '[class]': 'sizeClass()',
        '[attr.data-variant]': 'variant()',
        '[style.--dot-color-icon-color]': 'cssColor()'
    },
    styles: `
        :host {
            --dot-color-icon-bg: color-mix(in srgb, var(--dot-color-icon-color) 15%, transparent);
            --dot-color-icon-fg: var(--dot-color-icon-color);

            background-color: var(--dot-color-icon-bg);
            color: var(--dot-color-icon-fg);
        }

        :host([data-variant='solid']) {
            --dot-color-icon-bg: var(--dot-color-icon-color);
            --dot-color-icon-fg: white;
            /*
             * Progressive enhancement: pick black or white based on the XYZ Y channel
             * (relative luminance) of the background. Threshold from Lea Verou's formula —
             * see https://piccalil.li/blog/some-css-only-contrast-options-until-contrast-color-is-baseline-widely-available/
             * Browsers without relative color syntax support fall back to the 'white' above.
             */
            --dot-color-icon-fg: color(
                from var(--dot-color-icon-color) xyz clamp(0, (0.36 / y - 1) * infinity, 1)
                    clamp(0, (0.36 / y - 1) * infinity, 1) clamp(0, (0.36 / y - 1) * infinity, 1) /
                    1
            );
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotColorIconComponent {
    color = input.required<string>();
    variant = input<'light' | 'solid'>('light');
    size = input<'sm' | 'md'>('md');

    protected sizeClass = computed(() => (this.size() === 'sm' ? 'w-12 h-12' : 'w-14 h-14'));

    /**
     * Resolves the color input into a CSS color value.
     * - Hex strings (e.g. `#3b82f6`) are used as-is so consumers can pass brand colors at runtime.
     * - Any other token (e.g. `blue`, `surface`) resolves to the matching PrimeNG CSS variable.
     */
    protected cssColor = computed(() => {
        const value = this.color();

        return HEX_COLOR_PATTERN.test(value) ? value : `var(--p-${value}-500)`;
    });
}
