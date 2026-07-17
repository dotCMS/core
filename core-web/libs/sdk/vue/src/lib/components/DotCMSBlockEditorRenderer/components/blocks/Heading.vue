<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Renders a heading block as h1..h6 based on `node.attrs.level` (default 1). */
const props = defineProps<{ node: BlockEditorNode }>();

const tag = computed(() => `h${props.node?.attrs?.level || 1}`);
</script>

<template>
    <component :is="tag">
        <slot />
    </component>
</template>
