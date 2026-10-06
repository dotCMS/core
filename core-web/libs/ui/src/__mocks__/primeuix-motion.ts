import { vi } from 'vitest';
export const ANIMATION = 'animation';
export const TRANSITION = 'transition';
export const DEFAULT_MOTION_OPTIONS = {
    name: 'p',
    safe: true,
    disabled: false,
    enter: true,
    leave: true,
    autoHeight: true,
    autoWidth: false
};

interface MotionEvent {
    element: unknown;
}

type MotionHook = ((event: MotionEvent) => void) | undefined;

interface MotionOptions {
    onBeforeEnter?: MotionHook;
    onEnter?: MotionHook;
    onAfterEnter?: MotionHook;
    onBeforeLeave?: MotionHook;
    onLeave?: MotionHook;
    onAfterLeave?: MotionHook;
}

/**
 * Mirrors the real `createMotion`'s reduced-motion fast path: no CSS animation runs, but the
 * before/start/after hooks still fire synchronously with `{ element }`.
 *
 * Firing them matters — PrimeNG overlays emit their public `onShow` / `onHide` from these hooks
 * (`Popover.onAnimationStart` is bound to `pMotionOnEnter`), so a mock that swallowed them left
 * every `(onShow)` handler dead in tests.
 */
export const createMotion = (element: unknown, options: MotionOptions = {}) => {
    const run = (before: MotionHook, start: MotionHook, after: MotionHook): Promise<void> => {
        const event: MotionEvent = { element };

        before?.(event);
        start?.(event);
        after?.(event);

        return Promise.resolve();
    };

    return {
        enter: vi.fn(() => run(options.onBeforeEnter, options.onEnter, options.onAfterEnter)),
        leave: vi.fn(() => run(options.onBeforeLeave, options.onLeave, options.onAfterLeave)),
        cancel: vi.fn(),
        update: vi.fn()
    };
};

export const getMotionHooks = vi.fn();
export const getMotionMetadata = vi.fn();
export const mergeOptions = vi.fn();
export const resolveClassNames = vi.fn();
export const resolveDuration = vi.fn();
export const setAutoDimensionVariables = vi.fn();
export const shouldSkipMotion = vi.fn();
