import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
    selector: 'dot-color-icon',
    standalone: true,
    template: `
        <ng-content />
    `,
    host: {
        class: 'rounded-xl flex items-center justify-center shrink-0',
        '[class]': 'sizeClass()',
        '[style.background-color]': 'bgColor()',
        '[style.color]': 'fgColor()'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotColorIconComponent {
    color = input.required<string>();
    variant = input<'light' | 'solid'>('light');
    size = input<'sm' | 'md'>('md');

    protected sizeClass = computed(() => (this.size() === 'sm' ? 'w-12 h-12' : 'w-14 h-14'));
    protected bgColor = computed(() =>
        this.variant() === 'solid' ? `var(--p-${this.color()}-500)` : `var(--p-${this.color()}-100)`
    );
    protected fgColor = computed(() =>
        this.variant() === 'solid' ? 'white' : `var(--p-${this.color()}-500)`
    );
}
