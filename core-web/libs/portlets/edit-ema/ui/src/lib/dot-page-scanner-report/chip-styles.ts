/** CSS custom property overrides for p-chip color variants */
export const CHIP_STYLES = {
    red: { '--p-chip-background': 'var(--p-red-100)', '--p-chip-color': 'var(--p-red-700)' },
    yellow: {
        '--p-chip-background': 'var(--p-yellow-100)',
        '--p-chip-color': 'var(--p-yellow-700)'
    },
    blue: { '--p-chip-background': 'var(--p-blue-100)', '--p-chip-color': 'var(--p-blue-700)' },
    green: { '--p-chip-background': 'var(--p-green-100)', '--p-chip-color': 'var(--p-green-800)' }
} as const;
