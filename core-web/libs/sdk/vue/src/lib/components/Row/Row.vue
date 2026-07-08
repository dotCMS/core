<script setup lang="ts">
import { computed } from 'vue';

import type { DotPageAssetLayoutRow } from '@dotcms/types';
import { combineClasses, DOT_SECTION_ID_PREFIX } from '@dotcms/uve/internal';

import Column from '../Column/Column.vue';

/**
 * @internal
 * Renders a layout row and its columns. The `id` is the section anchor the
 * editor scrolls to; the inner grid spans 12 columns.
 */
const props = defineProps<{
    row: DotPageAssetLayoutRow;
    /** 1-based section index used as the scroll-to-section anchor id. */
    index: number;
}>();

const rowClass = computed(() => combineClasses(['dot-row-container', props.row.styleClass || '']));
const sectionId = computed(() => `${DOT_SECTION_ID_PREFIX}${props.index}`);
</script>

<template>
  <div
    :id="sectionId"
    :class="rowClass"
  >
    <div
      class="dotcms-row"
      data-dot-object="row"
    >
      <Column
        v-for="(column, i) in row.columns"
        :key="i"
        :column="column"
      />
    </div>
  </div>
</template>

<style scoped>
.dotcms-row {
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 1rem;
}
</style>
