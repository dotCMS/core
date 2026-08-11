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
 * and sizes option padding + checkbox to the chip-filter design spec.
 */
export const CHIP_FILTER_LISTBOX_PT = {
    root: {
        class: [
            '!border-0 !rounded-none !shadow-none',
            '[--p-listbox-option-padding:0_1rem]',
            '[--p-listbox-option-focus-background:var(--p-slate-50)]',
            '[--p-listbox-option-selected-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-background:var(--p-listbox-option-selected-background)]',
            '[--p-checkbox-width:16px] [--p-checkbox-height:16px]'
        ].join(' ')
    }
};
