import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
    selector: 'dot-filter-list-item',
    standalone: true,
    template: `
        <span class="text-md truncate font-medium">{{ label() }}</span>
        @if (secondary(); as s) {
            <span class="text-sm text-slate-500 truncate">({{ s }})</span>
        }
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center gap-2 w-full min-w-0 py-3'
    }
})
export class DotFilterListItemComponent {
    label = input.required<string>();
    secondary = input<string>();
}
