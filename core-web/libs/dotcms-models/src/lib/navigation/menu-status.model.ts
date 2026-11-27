/**
 * Where the navigation menu is in its load cycle.
 *
 * - `idle`: nothing requested yet in this session.
 * - `loading`: a request is in flight (entities that were already showing stay on screen).
 * - `ready`: the last request succeeded and its menu is the snapshot on screen.
 * - `error`: the last request failed; entities that were showing, if any, stay on screen.
 */
export const MenuStatus = {
    idle: 'idle',
    loading: 'loading',
    ready: 'ready',
    error: 'error'
} as const;

export type MenuStatus = (typeof MenuStatus)[keyof typeof MenuStatus];
