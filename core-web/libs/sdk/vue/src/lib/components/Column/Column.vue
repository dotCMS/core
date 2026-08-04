<script setup lang="ts">
import { computed, type CSSProperties } from 'vue';

import type { DotPageAssetLayoutColumn } from '@dotcms/types';

import Container from '../Container/Container.vue';

/**
 * @internal
 * Renders a single column of the 12-column grid. Position is derived from the
 * column's `leftOffset`/`width` — the same values the React SDK turns into
 * `col-start-*`/`col-end-*` classes, applied here as inline grid styles.
 */
const props = defineProps<{ column: DotPageAssetLayoutColumn }>();

const gridStyle = computed<CSSProperties>(() => ({
    gridColumnStart: props.column.leftOffset,
    gridColumnEnd: props.column.leftOffset + props.column.width
}));
</script>

<template>
  <div
    data-dot="column"
    :style="gridStyle"
  >
    <div :class="column.styleClass">
      <Container
        v-for="container in column.containers"
        :key="`${container.identifier}-${container.uuid}`"
        :container="container"
      />
    </div>
  </div>
</template>
