<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Custom block-editor renderer for embedded Destination contentlets. */
const props = defineProps<{ node: BlockEditorNode }>();

const data = computed(
    () =>
        (props.node.attrs?.data ?? {}) as {
            title?: string;
            shortDescription?: string;
            contentType?: string;
        }
);
</script>

<template>
    <div class="p-6 mb-4 rounded-2xl bg-white shadow-lg">
        <h2 class="text-2xl font-bold">{{ data.title }}</h2>
        <p class="line-clamp-2">{{ data.shortDescription }}</p>
        <p class="text-sm text-indigo-700">{{ data.contentType }}</p>
    </div>
</template>
