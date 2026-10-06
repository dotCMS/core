<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

/** Custom block-editor renderer for embedded Product contentlets. */
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
        <!-- eslint-disable-next-line vue/no-v-html -- renders CMS-authored product copy -->
        <div class="line-clamp-2" v-html="data.description ?? ''" />
        <p class="text-sm text-blue-500">{{ data.contentType }}</p>
    </div>
</template>
