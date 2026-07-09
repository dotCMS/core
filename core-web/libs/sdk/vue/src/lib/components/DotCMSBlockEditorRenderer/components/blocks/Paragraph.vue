<script setup lang="ts">
import { computed, type CSSProperties } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Renders a paragraph block, applying any node attrs as inline styles. */
const props = defineProps<{ node: BlockEditorNode }>();

const style = computed(() => (props.node?.attrs ?? {}) as CSSProperties);
</script>

<template>
  <p :style="style">
    <slot />
  </p>
</template>
