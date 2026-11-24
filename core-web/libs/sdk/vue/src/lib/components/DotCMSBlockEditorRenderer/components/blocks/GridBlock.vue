<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

import BlockEditorBlock from '../BlockEditorBlock.vue';

import type { CustomRenderer } from '../../types';

/** Renders a two-column grid block over a 12-column grid. */
const props = defineProps<{
    node: BlockEditorNode;
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}>();

const cols = computed<number[]>(() => {
    const raw = props.node.attrs?.columns;

    return Array.isArray(raw) &&
        raw.length === 2 &&
        raw.every((v: unknown) => typeof v === 'number' && Number.isFinite(v))
        ? (raw as number[])
        : [6, 6];
});

const gridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(12, 1fr)',
    gap: '1rem'
} as const;
</script>

<template>
  <div
    data-type="gridBlock"
    class="grid-block"
    :style="gridStyle"
  >
    <div
      v-for="(column, index) in node.content ?? []"
      :key="`gridColumn-${index}`"
      data-type="gridColumn"
      class="grid-block__column"
      :style="{ gridColumn: `span ${cols[index] ?? 6}` }"
    >
      <BlockEditorBlock
        :content="column.content"
        :custom-renderers="customRenderers"
        :is-dev-mode="isDevMode"
      />
    </div>
  </div>
</template>
