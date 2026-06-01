import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

const BASE_CLASSES =
    'flex items-center justify-between gap-2 px-3 py-1.5 rounded-full text-sm font-normal leading-normal cursor-pointer select-none whitespace-nowrap min-w-[140px] transition-[color,background-color,border-color,width] duration-200 ease-out';

const INACTIVE_CLASSES = 'bg-white text-slate-600 border border-slate-200 hover:border-primary-400';

const ACTIVE_CLASSES =
    'bg-primary-100 text-primary-900 border border-transparent hover:bg-primary-200';

@Component({
    selector: 'dot-chip-filter',
    imports: [DotMessagePipe],
    templateUrl: './dot-chip-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class]': 'stateClasses()',
        role: 'button',
        '[attr.tabindex]': 'tabIndex()',
        '(click)': 'clicked.emit($event)',
        '(keydown.enter)': 'onHostKeydown($event)',
        '(keydown.space)': 'onHostKeydown($event)'
    }
})
export class DotChipFilterComponent {
    readonly #dotMessageService = inject(DotMessageService);

    title = input.required<string>();
    selections = input<string[]>([]);
    tabIndex = input<number>(0);

    /**
     * Emits the originating DOM event so consumers can pass it to overlays
     * (e.g. p-popover) that need positioning info from `currentTarget`.
     */
    clicked = output<Event>();
    removed = output<void>();

    protected readonly active = computed(() => this.selections().length > 0);

    protected readonly valuesLabel = computed(() => {
        const selections = this.selections();

        if (!selections.length) return '';
        if (selections.length <= 2) return selections.join(', ');

        return this.#dotMessageService.get(
            'content-drive.chip-filter.overflow-label',
            selections[0],
            String(selections.length - 1)
        );
    });

    protected readonly stateClasses = computed(
        () => `${BASE_CLASSES} ${this.active() ? ACTIVE_CLASSES : INACTIVE_CLASSES}`
    );

    protected onRemove(event: Event): void {
        event.stopPropagation();
        this.removed.emit();
    }

    protected onHostKeydown(event: Event): void {
        // Ignore keydowns that bubbled from a descendant (e.g. the close button)
        if (event.target && event.target !== event.currentTarget) return;
        if ((event as KeyboardEvent).key === ' ') event.preventDefault();
        this.clicked.emit(event);
    }
}
