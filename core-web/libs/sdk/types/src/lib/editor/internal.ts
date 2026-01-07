import { DotCMSUVEAction } from './public';

/**
 * Configuration for reordering a menu.
 */
export interface DotCMSReorderMenuConfig {
    /**
     * The starting level of the menu to be reordered.
     */
    startLevel: number;

    /**
     * The depth of the menu levels to be reordered.
     */
    depth: number;
}

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

/**
 * Bound information for a contentlet.
 *
 * @interface ContentletBound
 * Bound information for a contentlet.
 *
 * @interface DotCMSContentletBound
 * @property {number} x - The x-coordinate of the contentlet.
 * @property {number} y - The y-coordinate of the contentlet.
 * @property {number} width - The width of the contentlet.
 * @property {number} height - The height of the contentlet.
 * @property {string} payload - The payload data of the contentlet in stringified JSON format.
 */
export interface DotCMSContentletBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
}

/**
 * Bound information for a container.
 *
 * @interface DotCMSContainerBound
 * @property {number} x - The x-coordinate of the container.
 * @property {number} y - The y-coordinate of the container.
 * @property {number} width - The width of the container.
 * @property {number} height - The height of the container.
 * @property {string} payload - The payload data of the container in JSON format.
 * @property {DotCMSContentletBound[]} contentlets - An array of contentlets within the container.
 */
export interface DotCMSContainerBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
    contentlets: DotCMSContentletBound[];
}

/**
 *
 * Interface representing the data attributes of a DotCMS container.
 * @interface DotContainerAttributes
 */
export interface DotContainerAttributes {
    'data-dot-object': string;
    'data-dot-accept-types': string;
    'data-dot-identifier': string;
    'data-max-contentlets': string;
    'data-dot-uuid': string;
}

/**
 *
 * Interface representing the data attributes of a DotCMS contentlet.
 * @interface DotContentletAttributes
 */
export interface DotContentletAttributes {
    'data-dot-identifier': string;
    'data-dot-basetype': string;
    'data-dot-title': string;
    'data-dot-inode': string;
    'data-dot-type': string;
    'data-dot-container': string;
    'data-dot-on-number-of-pages': string;
}
