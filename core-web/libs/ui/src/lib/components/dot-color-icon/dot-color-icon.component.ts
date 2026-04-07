import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
    selector: 'dot-color-icon',
    standalone: true,
    template: `<ng-content />`,
    host: {
        class: 'w-14 h-14 rounded-2xl flex items-center justify-center shrink-0',
        '[style.background-color]': 'bgColor()',
        '[style.color]': 'fgColor()'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotColorIconComponent {
    color = input.required<string>();

    protected bgColor = computed(() => `var(--p-${this.color()}-100)`);
    protected fgColor = computed(() => `var(--p-${this.color()}-500)`);
}
