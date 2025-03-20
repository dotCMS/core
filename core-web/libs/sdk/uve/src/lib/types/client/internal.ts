import { DotCMSUVEAction } from './public';

declare global {
    interface Window {
        dotCMSUVE: DotCMSUVE;
    }
}

/**
 * Post message props
 *
 * @export
 * @template T
 * @interface DotCMSUVEMessage
 */
export type DotCMSUVEMessage<T> = {
    action: DotCMSUVEAction;
    payload?: T;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type DotCMSUVEFunction = (...args: any[]) => void;

export interface DotCMSUVE {
    editContentlet: DotCMSUVEFunction;
    initInlineEditing: DotCMSUVEFunction;
    reorderMenu: DotCMSUVEFunction;
    lastScrollYPosition: number;
}
