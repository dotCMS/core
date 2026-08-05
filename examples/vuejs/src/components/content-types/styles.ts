import { cva } from 'class-variance-authority';

/** Allowed `button-color` style values, shared by Banner and Activity. */
export const BUTTON_COLORS = ['blue', 'green', 'red', 'purple', 'orange', 'teal'] as const;

export type ButtonColor = (typeof BUTTON_COLORS)[number];

/**
 * Shared button color variants used by Banner and Activity. The `button-color`
 * style property selects one of these; both components reuse the same palette.
 */
export const buttonColorVariants = cva('', {
    variants: {
        color: {
            blue: 'bg-blue-500 hover:bg-blue-700',
            green: 'bg-green-500 hover:bg-green-700',
            red: 'bg-red-500 hover:bg-red-700',
            purple: 'bg-purple-500 hover:bg-purple-700',
            orange: 'bg-orange-500 hover:bg-orange-700',
            teal: 'bg-teal-500 hover:bg-teal-700'
        }
    },
    defaultVariants: {
        color: 'blue'
    }
});
