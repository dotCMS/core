import { DotClientActionType } from './public';

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
 * @interface DotCMSPostMessageProps
 */
type DotCMSPostMessageProps<T> = {
    action: DotClientActionType;
    payload?: T;
};

/**
 * Post message to dotcms page editor
 *
 * @export
 * @template T
 * @param {DotCMSPostMessageProps<T>} message
 */
export function sendPostMessageToEditor<T = unknown>(message: DotCMSPostMessageProps<T>) {
    window.parent.postMessage(message, '*');
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type DotCMSUVEFunction = (...args: any[]) => void;

export interface DotCMSUVE {
    editContentlet: DotCMSUVEFunction;
    initInlineEditing: DotCMSUVEFunction;
    reorderMenu: DotCMSUVEFunction;
    lastScrollYPosition: number;
}

/**
 * Represents a listener for DotCMSClientListener.
 *
 * @typedef {Object} DotCMSClientListener
 * @property {string} action - The action that triggers the event.
 * @property {string} event - The name of the event.
 * @property {function(...args: any[]): void} callback - The callback function to handle the event.
 */
export type DotCMSClientListener = {
    action: string;
    event: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    callback: (...args: any[]) => void;
};
