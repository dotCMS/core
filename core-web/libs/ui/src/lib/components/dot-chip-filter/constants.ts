/**
 * Pass-through styling for the popover that hosts a chip-filter listbox.
 * Removes default content padding and rounds the corners.
 */
export const CHIP_FILTER_POPOVER_PT = {
    root: { class: '!rounded-lg overflow-hidden' },
    content: { class: '!p-0' }
};

/**
 * Pass-through styling for the listbox rendered inside a chip-filter popover.
 * Strips the listbox's own chrome, applies palette colors for selection/hover,
 * and sizes the checkbox to the chip-filter design spec.
 *
 * Option padding is deliberately NOT set here. The theme now fixes every listbox option to
 * Lara's `0.625rem 1rem`, which is what `LISTBOX_OPTION_HEIGHT` measures; overriding it to
 * `0 1rem` here would make every virtual scroller's row height disagree with what is rendered.
 * The remaining declarations duplicate the theme and are kept only to avoid a wider change
 * during a merge — see the follow-up note in the PR.
 */
export const CHIP_FILTER_LISTBOX_PT = {
    root: {
        class: [
            '!border-0 !rounded-none !shadow-none',
            '[--p-listbox-option-focus-background:var(--p-slate-50)]',
            '[--p-listbox-option-selected-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-background:var(--p-listbox-option-selected-background)]',
            '[--p-checkbox-width:16px] [--p-checkbox-height:16px]'
        ].join(' ')
    }
};

/** Max height of a listbox rendered inside a chip-filter popover. */
export const CHIP_FILTER_SCROLL_HEIGHT = '25rem';
