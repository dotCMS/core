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

export const createMotion = () => ({
    enter: jest.fn().mockResolvedValue(undefined),
    leave: jest.fn().mockResolvedValue(undefined),
    cancel: jest.fn()
});

export const getMotionHooks = jest.fn();
export const getMotionMetadata = jest.fn();
export const mergeOptions = jest.fn();
export const resolveClassNames = jest.fn();
export const resolveDuration = jest.fn();
export const setAutoDimensionVariables = jest.fn();
export const shouldSkipMotion = jest.fn();
