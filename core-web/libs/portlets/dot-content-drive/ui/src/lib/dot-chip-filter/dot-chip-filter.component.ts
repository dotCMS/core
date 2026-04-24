import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Component({
    selector: 'dot-chip-filter',
    standalone: true,
    imports: [],
    templateUrl: './dot-chip-filter.component.html',
    styleUrl: './dot-chip-filter.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center justify-between gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium cursor-pointer transition-all border whitespace-nowrap bg-white text-slate-600 border-slate-200',
        '[class.active]': 'active()',
        '(click)': 'clicked.emit()'
    }
})
export class DotChipFilterComponent {
    readonly #dotMessageService = inject(DotMessageService);

    title = input.required<string>();
    selections = input<string[]>([]);

    clicked = output<void>();
    removed = output<void>();

    protected readonly label = computed(() => {
        const selections = this.selections();
        const title = this.title();

        if (!selections.length) return title;
        if (selections.length <= 2) return `${title}: ${selections.join(', ')}`;

        const and = this.#dotMessageService.get('and').toLowerCase();
        const more = this.#dotMessageService.get('more').toLowerCase();

        return `${title}: ${selections[0]} ${and} ${selections.length - 1} ${more}...`;
    });

    protected readonly active = computed(() => {
        return this.selections().length;
    });

    onRemove(event: MouseEvent): void {
        event.stopPropagation();
        this.removed.emit();
    }
}
