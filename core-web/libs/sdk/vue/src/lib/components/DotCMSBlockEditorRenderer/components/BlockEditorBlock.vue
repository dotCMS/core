<script setup lang="ts">
import type { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';

import BulletList from './blocks/BulletList.vue';
import CodeBlock from './blocks/CodeBlock.vue';
import DotContent from './blocks/DotContent.vue';
import DotImage from './blocks/DotImage.vue';
import DotVideo from './blocks/DotVideo.vue';
import GridBlock from './blocks/GridBlock.vue';
import Heading from './blocks/Heading.vue';
import ListItem from './blocks/ListItem.vue';
import OrderedList from './blocks/OrderedList.vue';
import Paragraph from './blocks/Paragraph.vue';
import Quote from './blocks/Quote.vue';
import TableRenderer from './blocks/TableRenderer.vue';
import TextBlock from './blocks/TextBlock.vue';
import UnknownBlock from './blocks/UnknownBlock.vue';

import type { CustomRenderer } from '../types';

/**
 * Recursive dispatcher: renders a list of block-editor nodes, delegating each to
 * a custom renderer (if provided) or the matching built-in block component.
 */
defineProps<{
    content?: BlockEditorNode[];
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}>();

const Blocks = BlockEditorDefaultBlocks;

/** Node types that render their children via a nested dispatcher in the default slot. */
const CONTAINER_BLOCKS: Record<string, unknown> = {
    [Blocks.PARAGRAPH]: Paragraph,
    [Blocks.HEADING]: Heading,
    [Blocks.BULLET_LIST]: BulletList,
    [Blocks.ORDERED_LIST]: OrderedList,
    [Blocks.LIST_ITEM]: ListItem,
    [Blocks.BLOCK_QUOTE]: Quote,
    [Blocks.CODE_BLOCK]: CodeBlock
};

/** Node types that render themselves from attrs with no children slot. */
const LEAF_BLOCKS: Record<string, unknown> = {
    [Blocks.DOT_IMAGE]: DotImage,
    [Blocks.DOT_VIDEO]: DotVideo
};

const containerComponent = (type: string) => CONTAINER_BLOCKS[type];
const leafComponent = (type: string) => LEAF_BLOCKS[type];
</script>

<template>
    <template v-for="(node, index) in content ?? []" :key="`${node.type}-${index}`">
        <!-- Custom renderer takes precedence for any matching block type. -->
        <component
            :is="customRenderers[node.type]"
            v-if="customRenderers && customRenderers[node.type]"
            :node="node">
            <BlockEditorBlock
                :content="node.content"
                :custom-renderers="customRenderers"
                :is-dev-mode="isDevMode" />
        </component>

        <!-- Container blocks: wrap a nested dispatcher of their children. -->
        <component
            :is="containerComponent(node.type)"
            v-else-if="containerComponent(node.type)"
            :node="node">
            <BlockEditorBlock
                :content="node.content"
                :custom-renderers="customRenderers"
                :is-dev-mode="isDevMode" />
        </component>

        <!-- Text leaf: applies marks. -->
        <TextBlock v-else-if="node.type === Blocks.TEXT" :text="node.text" :marks="node.marks" />

        <!-- Attr-driven leaf blocks. -->
        <component
            :is="leafComponent(node.type)"
            v-else-if="leafComponent(node.type)"
            :node="node" />

        <!-- Void blocks. -->
        <br v-else-if="node.type === Blocks.HARDBREAK" />
        <hr v-else-if="node.type === Blocks.HORIZONTAL_RULE" />

        <!-- Complex blocks that recurse internally. -->
        <TableRenderer
            v-else-if="node.type === Blocks.TABLE"
            :content="node.content ?? []"
            :attrs="node.attrs"
            :custom-renderers="customRenderers"
            :is-dev-mode="isDevMode" />
        <GridBlock
            v-else-if="node.type === Blocks.GRID_BLOCK"
            :node="node"
            :custom-renderers="customRenderers"
            :is-dev-mode="isDevMode" />
        <DotContent
            v-else-if="node.type === Blocks.DOT_CONTENT"
            :node="node"
            :custom-renderers="customRenderers"
            :is-dev-mode="isDevMode" />

        <!-- Anything else. -->
        <UnknownBlock v-else :node="node" />
    </template>
</template>
