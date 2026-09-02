<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Renders a code block, exposing the language via `data-language`. */
const props = defineProps<{ node: BlockEditorNode }>();

const language = computed(() => props.node?.attrs?.language || '');
</script>

<template>
    <pre :data-language="language"><code><slot /></code></pre>
</template>
