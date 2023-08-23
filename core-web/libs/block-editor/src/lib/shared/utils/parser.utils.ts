import { Content, JSONContent } from '@tiptap/core';

interface BlockMap {
    [key: string]: boolean;
}

// If we allowed tables, we must allow its dependencies.
const tableContent: BlockMap = {
    table: true,
    tableRow: true,
    tableHeader: true,
    tableCell: true
};

const orderedListContent: BlockMap = {
    orderedList: true,
    listItem: true
};

const bulletListContent: BlockMap = {
    bulletList: true,
    listItem: true
};

const imageContent: BlockMap = {
    image: true,
    dotImage: true
};

const video: BlockMap = {
    dotVideo: true,
    youtube: true
};

const basicNodes: BlockMap = { paragrah: true, text: true, doc: true };

const relatedContent = {
    image: imageContent,
    table: tableContent,
    orderedList: orderedListContent,
    bulletList: bulletListContent,
    video
};

/**
 * Check is the current node is Heading type
 *
 * @param {*} node
 * @param {*} blocksMap
 * @return {*}
 */
const isHeading = (node, blocksMap) => {
    const { type, attrs } = node;
    if (type !== 'heading') {
        return false;
    }

    // Make sure to check the the type + the level so it can macth the mao.
    return blocksMap[type + attrs.level];
};

export const removeInvalidNodes = (data: Content, allowedBlocks: string[]) => {
    const blocksMap = getBlockMap(allowedBlocks);
    const content = Array.isArray(data) ? [...data] : [...(data as JSONContent).content];

    return purifyNodeTree(content, blocksMap);
};

/**
 *
 *
 * @param {*} content
 * @param {*} blocksMap
 * @return {*}
 */
export const purifyNodeTree = (content: JSONContent[], blocksMap: BlockMap): JSONContent[] => {
    if (!content?.length) {
        return content;
    }

    const allowedContent = [];

    for (const i in content) {
        const node = content[i];

        if (blocksMap[node.type] || isHeading(node, blocksMap)) {
            allowedContent.push({
                ...node,
                content: purifyNodeTree(node.content, blocksMap)
            });
        }
    }

    return allowedContent;
};

/**
 * Convert the allowBlock array to an object.
 * Since we are going to traverse a tree of nodes (using recursion),
 * it is preferable to use an object to determine if the node should belong to the tree [O(1)].
 *
 * @param {string[]} allowedBlock
 * @return {*}
 */
export const getBlockMap = (allowedBlock: string[]): BlockMap => {
    return allowedBlock.reduce((blocks, current) => {
        if (relatedContent[current]) {
            return {
                ...blocks,
                ...relatedContent[current]
            };
        }

        return { ...blocks, [current]: true };
    }, basicNodes);
};
