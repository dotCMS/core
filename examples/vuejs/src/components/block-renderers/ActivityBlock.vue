<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Custom block-editor renderer for embedded Activity contentlets. */
const props = defineProps<{ node: BlockEditorNode }>();

const data = computed(
    () =>
        (props.node.attrs?.data ?? {}) as {
            title?: string;
            description?: string;
            contentType?: string;
        }
);
</script>

<template>
    <div class="p-6 mb-4 overflow-hidden rounded-2xl bg-white shadow-lg">
        <h2 class="text-2xl font-bold">{{ data.title }}</h2>
        <p class="line-clamp-2">{{ data.description }}</p>
        <p class="text-sm text-cyan-700">{{ data.contentType }}</p>
    </div>
</template>
