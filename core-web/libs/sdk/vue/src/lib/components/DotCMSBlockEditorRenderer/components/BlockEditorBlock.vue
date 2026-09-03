<script setup lang="ts">
import { computed } from 'vue';

import { groupLinkRuns, resolveEmoji, type EmojiWarnScope } from '@dotcms/client/internal';
import type { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';

import BulletList from './blocks/BulletList.vue';
import CodeBlock from './blocks/CodeBlock.vue';
import DotAudio from './blocks/DotAudio.vue';
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
import LinkRun from './blocks/LinkRun.vue';
import TextBlock from './blocks/TextBlock.vue';
import UnknownBlock from './blocks/UnknownBlock.vue';

import type { CustomRenderer } from '../types';

/**
 * Recursive dispatcher: renders a list of block-editor nodes, delegating each to
 * a custom renderer (if provided) or the matching built-in block component.
 */
const props = defineProps<{
    content?: BlockEditorNode[];
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}>();

// One scope per render so a repeated unresolvable emoji warns once, not once per occurrence.
const warned: EmojiWarnScope = new Set<string>();

/**
 * Group siblings into link runs before rendering (#37340).
 *
 * Rendering node-by-node emits one `<a>` per text node, so a link split by a legacy `emoji` node
 * became two anchors — two tab stops and two screen-reader entries for one logical link. The
 * `link` mark is stripped from the children of a run so `TextBlock` does not emit a second,
 * nested anchor inside the one `LinkRun` already provides.
 */
const groups = computed(() =>
    groupLinkRuns(props.content ?? []).map((group) => ({
        link: group.link,
        nodes: group.link
            ? group.nodes.map((node) => ({
                  ...node,
                  marks: node.marks?.filter((mark) => mark.type !== 'link')
              }))
            : group.nodes
    }))
);

const emojiText = (node: BlockEditorNode) => resolveEmoji(node, warned);

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
    [Blocks.DOT_VIDEO]: DotVideo,
    [Blocks.DOT_AUDIO]: DotAudio
};

const containerComponent = (type: string) => CONTAINER_BLOCKS[type];
const leafComponent = (type: string) => LEAF_BLOCKS[type];
</script>

<template>
    <template v-for="(group, groupIndex) in groups" :key="groupIndex">
        <LinkRun :link="group.link">
            <template v-for="(node, index) in group.nodes" :key="`${node.type}-${index}`">
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
                <TextBlock
                    v-else-if="node.type === Blocks.TEXT"
                    :text="node.text"
                    :marks="node.marks" />

                <!--
            Legacy content only — the editor no longer creates `emoji` nodes (#37340). The node
            stores a shortcode, never the character, so without this branch it fell through to
            UnknownBlock and rendered nothing on the live site.
        -->
                <span v-else-if="node.type === Blocks.EMOJI">{{ emojiText(node) }}</span>

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
        </LinkRun>
    </template>
</template>
