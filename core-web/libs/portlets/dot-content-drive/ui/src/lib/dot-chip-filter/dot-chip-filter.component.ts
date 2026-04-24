import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

const INACTIVE_CLASSES = 'bg-white text-slate-600 border-slate-200 hover:border-primary-400';

const ACTIVE_CLASSES =
    'bg-primary-100 text-primary-700 border-primary-400 hover:bg-primary-200 hover:text-primary-800 hover:border-primary-500';

@Component({
    selector: 'dot-chip-filter',
    standalone: true,
    imports: [],
    templateUrl: './dot-chip-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center justify-between gap-2 px-3 py-1.5 rounded-full text-sm font-medium leading-normal cursor-pointer transition-all border whitespace-nowrap min-w-[140px]',
        '[class]': 'stateClasses()',
        '(click)': 'clicked.emit()'
    }
})
export class DotChipFilterComponent {
    readonly #dotMessageService = inject(DotMessageService);

    title = input.required<string>();
    selections = input<string[]>([]);

    clicked = output<void>();
    removed = output<void>();

    protected readonly active = computed(() => this.selections().length > 0);

    protected readonly valuesLabel = computed(() => {
        const selections = this.selections();

        if (!selections.length) return '';
        if (selections.length <= 2) return selections.join(', ');

        const and = this.#dotMessageService.get('and').toLowerCase();
        const more = this.#dotMessageService.get('more').toLowerCase();

        return `${selections[0]} ${and} ${selections.length - 1} ${more}`;
    });

    protected readonly stateClasses = computed(() =>
        this.active() ? ACTIVE_CLASSES : INACTIVE_CLASSES
    );

    onRemove(event: MouseEvent): void {
        event.stopPropagation();
        this.removed.emit();
    }
}
