import { DotCMSContainer, DotCMSPageAssetContainer } from '../models/dotcms.model';

//Changed the type, to avoid SQ issue.
//This should be put inside a lib
/**
 * Represents a mapping of numbers to corresponding CSS class names for column end values.
 * @typedef {Record<number, string | null>} EndClassMap
 */
const endClassMap: Record<number, string | null> = {
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

//Changed the type, to avoid SQ issue.
//This should be put inside a lib
/**
 * Represents a mapping of numbers to CSS class names for starting columns.
 * @typedef {Record<number, string | null>} StartClassMap
 */
const startClassMap: Record<number, string | null> = {
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
 * Retrieves the data for a set of containers.
 *
 * @param containers - The DotCMSPageAssetContainer object containing the containers.
 * @param containerRef - The DotCMSContainer object representing the container reference.
 * @returns An object containing the container data, accept types, contentlets, and variant ID.
 */
export const getContainersData = (
    containers: DotCMSPageAssetContainer,
    containerRef: DotCMSContainer
) => {
    const { identifier, uuid } = containerRef;

    const { containerStructures, container } = containers[identifier];

    const { variantId } = container?.parentPermissionable || {};

    const acceptTypes: string = containerStructures
        .map((structure) => structure.contentTypeVar)
        .join(',');

    const contentlets = containers[identifier].contentlets[`uuid-${uuid}`];

    return {
        ...containers[identifier].container,
        acceptTypes,
        contentlets,
        variantId
    };
};

/**
 * Returns the position style classes based on the start and end values.
 * Used to set the grid column start and end values.
 * @param start - The start value.
 * @param end - The end value.
 * @returns An object containing the startClass and endClass.
 */
export const getPositionStyleClasses = (start: number, end: number) => {
    const startClass = startClassMap[start];
    const endClass = endClassMap[end];

    return {
        startClass,
        endClass
    };
};
