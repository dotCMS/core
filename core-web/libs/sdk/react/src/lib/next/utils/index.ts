import { DotCMSContentlet } from '../types';

const endClassMap: Record<number, string> = {
    1: 'col-end-1',
    2: 'col-end-2',
    3: 'col-end-3',
    4: 'col-end-4',
    5: 'col-end-5',
    6: 'col-end-6',
    7: 'col-end-7',
    8: 'col-end-8',
    9: 'col-end-9',
    10: 'col-end-10',
    11: 'col-end-11',
    12: 'col-end-12',
    13: 'col-end-13'
};

const startClassMap: Record<number, string> = {
    1: 'col-start-1',
    2: 'col-start-2',
    3: 'col-start-3',
    4: 'col-start-4',
    5: 'col-start-5',
    6: 'col-start-6',
    7: 'col-start-7',
    8: 'col-start-8',
    9: 'col-start-9',
    10: 'col-start-10',
    11: 'col-start-11',
    12: 'col-start-12'
};

/**
 * Combine classes into a single string.
 *
 * @param {string[]} classes
 * @returns {string} Combined classes
 */
export const combineClasses = (classes: string[]) => classes.filter(Boolean).join(' ');

/**
 * Get the start and end classes for the column based on the left offset and width.
 *
 * @param {number} start
 * @param {number} end
 * @returns {Object} Start and end classes
 */
export const getPositionStyleClasses = (start: number, end: number) => {
    const startClass = startClassMap[start];
    const endClass = endClassMap[end];

    return {
        startClass,
        endClass
    };
};

/**
 * Helper function that returns an object containing the dotCMS data attributes.
 */
export function getDotContentletAttributes(
    contentlet: DotCMSContentlet,
    container: string
): Record<string, any> {
    return {
        'data-dot-identifier': contentlet?.identifier,
        'data-dot-basetype': contentlet?.baseType,
        'data-dot-title': contentlet?.widgetTitle || contentlet?.title,
        'data-dot-inode': contentlet?.inode,
        'data-dot-type': contentlet?.contentType,
        'data-dot-container': container,
        'data-dot-on-number-of-pages': contentlet?.onNumberOfPages
    };
}
