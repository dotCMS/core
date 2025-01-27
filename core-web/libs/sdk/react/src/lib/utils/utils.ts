import { DotCMSContainer, DotCMSPageAssetContainer } from '@dotcms/types';

import { Block, BlockEditorState } from '../models/blocks.interface';

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
 * Get the container data from the containers object using the current container reference obtained from the layout.
 *
 * @param {ContainerData} containers
 * @param {DotCMSPageContext['pageAsset']['layout']['body']['rows'][0]['columns'][0]['containers'][0]} containerRef
 * @returns {Object} Container with all the data it has.
 */
export const getContainersData = (
    containers: DotCMSPageAssetContainer,
    containerRef: DotCMSContainer
) => {
    const { identifier, uuid } = containerRef;

    const { containerStructures, container } = containers[identifier];

    // Get the variant id
    const { variantId } = container?.parentPermissionable || {};

    // Get accepts types of content types for this container
    const acceptTypes = containerStructures.map((structure) => structure.contentTypeVar).join(',');

    // Get the contentlets for "this" container
    const contentlets =
        containers[identifier].contentlets[`uuid-${uuid}`] ??
        containers[identifier].contentlets[`uuid-dotParser_${uuid}`];

    if (!contentlets) {
        console.warn(
            `We couldn't find the contentlets for the container with the identifier ${identifier} and the uuid ${uuid} becareful by adding content to this container.\nWe recommend to change the container in the layout and add the content again.`
        );
    }

    return {
        ...containers[identifier].container,
        acceptTypes,
        contentlets: contentlets ?? [],
        variantId
    };
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
 * Validates the structure of a Block Editor block.
 *
 * This function checks that:
 * 1. The blocks parameter is a valid object
 * 2. The block has a 'doc' type
 * 3. The block has a valid content array that is not empty
 *
 * @param {Block} blocks - The blocks structure to validate
 * @returns {BlockEditorState} Object containing validation state and any error message
 * @property {boolean} BlockEditorState.isValid - Whether the blocks structure is valid
 * @property {string | null} BlockEditorState.error - Error message if invalid, null if valid
 */
export const isValidBlocks = (blocks: Block): BlockEditorState => {
    if (!blocks) {
        return {
            error: `Error: Blocks object is not defined`
        };
    }

    if (typeof blocks !== 'object') {
        return {
            error: `Error: Blocks must be an object, but received: ${typeof blocks}`
        };
    }

    if (blocks.type !== 'doc') {
        return {
            error: 'Error: Blocks must have a doc type'
        };
    }

    if (!blocks.content || !Array.isArray(blocks.content)) {
        return {
            error: 'Error: Blocks must have a valid content array'
        };
    }

    if (blocks.content.length === 0) {
        return {
            error: 'Error: Blocks content is empty'
        };
    }

    return {
        error: null
    };
};
