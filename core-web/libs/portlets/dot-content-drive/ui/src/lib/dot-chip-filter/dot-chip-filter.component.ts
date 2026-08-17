import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

const BASE_CLASSES =
    'flex items-center justify-between gap-2 px-3 py-1.5 rounded-full text-sm font-normal leading-normal cursor-pointer select-none whitespace-nowrap min-w-[140px] transition-[color,background-color,border-color,width] duration-200 ease-out';

const INACTIVE_CLASSES = 'bg-white text-slate-600 border border-slate-200 hover:border-primary-400';

const ACTIVE_CLASSES =
    'bg-primary-100 text-primary-900 border border-transparent hover:bg-primary-200';

/**
 * `dropdown` is the original chip: it fronts an overlay of options and its state comes from the
 * selections that overlay produced. `toggle` is a chip that *is* the control — a latching on/off
 * with no options behind it, so it carries no value label and no dropdown affordance.
 */
export type DotChipFilterMode = 'dropdown' | 'toggle';

@Component({
    selector: 'dot-chip-filter',
    imports: [DotMessagePipe],
    templateUrl: './dot-chip-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class]': 'stateClasses()',
        role: 'button',
        // Only a toggle has a pressed state to report; a dropdown chip's state lives in the overlay
        // it opens, so the attribute stays off it entirely rather than reporting a misleading value.
        '[attr.aria-pressed]': 'isToggle() ? active() : null',
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
     * Whether the chip offers its "remove" X while it has a selection. Defaults to `true`, which is
     * every chip whose filter can legitimately be emptied.
     *
     * Set it to `false` for a filter that always holds a value — the Locale chip while only the
     * environment default is selected — where an X would be a no-op: there is nothing to remove,
     * because clearing the filter re-selects that same default.
     */
    removable = input<boolean>(true);

    /**
     * Whether the chip fronts an overlay of options (`dropdown`, the default) or is itself a
     * latching on/off control (`toggle`). See {@link DotChipFilterMode}.
     */
    mode = input<DotChipFilterMode>('dropdown');

    /**
     * The on/off state, read only in `toggle` mode. A toggle has no selection strings to derive
     * its state from — being on is the state — so it cannot ride `selections` the way a dropdown
     * chip does without rendering a made-up value after the title.
     */
    toggled = input<boolean>(false);

    /**
     * Emits the originating DOM event so consumers can pass it to overlays
     * (e.g. p-popover) that need positioning info from `currentTarget`.
     */
    clicked = output<Event>();
    removed = output<void>();

    protected readonly isToggle = computed(() => this.mode() === 'toggle');

    protected readonly active = computed(() =>
        this.isToggle() ? this.toggled() : this.selections().length > 0
    );

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
