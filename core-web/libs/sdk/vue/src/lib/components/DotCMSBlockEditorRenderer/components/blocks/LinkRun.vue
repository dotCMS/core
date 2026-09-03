<script setup lang="ts">
import type { BlockEditorMark } from '@dotcms/types';

/**
 * Wraps a link run in a single `<a>` (#37340).
 *
 * Exists so the node template in `BlockEditorBlock.vue` is written once: without it, the whole
 * dispatcher would have to be duplicated for the linked and unlinked branches.
 *
 * When `link` is absent the children render bare, so unlinked content is untouched.
 */
defineProps<{ link?: BlockEditorMark }>();
</script>

<template>
  <a
    v-if="link"
    v-bind="link.attrs"
  >
    <slot />
  </a>
  <slot v-else />
</template>
