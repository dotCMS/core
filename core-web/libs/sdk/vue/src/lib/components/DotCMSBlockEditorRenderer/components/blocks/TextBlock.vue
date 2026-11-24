<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorMark } from '@dotcms/types';

/**
 * Renders a text node, applying its marks (bold, italic, link, ...) recursively.
 *
 * Each mark wraps a `TextBlock` of the remaining marks, so nested formatting is
 * preserved. When no marks remain the raw text is emitted.
 */
const props = defineProps<{
    text?: string;
    marks?: BlockEditorMark[];
}>();

/** Maps a mark type to the HTML tag that renders it. */
const MARK_TAG: Record<string, string> = {
    bold: 'strong',
    italic: 'em',
    strike: 's',
    underline: 'u',
    superscript: 'sup',
    subscript: 'sub',
    link: 'a'
};

const marks = computed<BlockEditorMark[]>(() => props.marks ?? []);
const currentMark = computed<BlockEditorMark | undefined>(() => marks.value[0]);
const remainingMarks = computed<BlockEditorMark[]>(() => marks.value.slice(1));

const tag = computed(() => (currentMark.value ? MARK_TAG[currentMark.value.type] : undefined));

// Mark attrs pass straight through (e.g. link href/target). Unlike React we keep
// `class` as-is — Vue binds it natively.
const attrs = computed(() => currentMark.value?.attrs ?? {});
</script>

<template>
  <component
    :is="tag"
    v-if="tag"
    v-bind="attrs"
  >
    <TextBlock
      :text="text"
      :marks="remainingMarks"
    />
  </component>
  <template v-else>
    {{ text }}
  </template>
</template>
