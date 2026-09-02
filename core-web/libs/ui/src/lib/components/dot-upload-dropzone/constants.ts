/**
 * Dropzone drag states. `INTERNAL_DRAG` distinguishes rows being dragged inside the host (a move,
 * not an upload) from files coming in from the OS.
 */
export const DROPZONE_STATE = {
    INTERNAL_DRAG: 'internal-drag',
    ACTIVE: 'active',
    INACTIVE: 'inactive'
} as const;
