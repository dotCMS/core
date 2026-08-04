<script setup lang="ts">
import { computed, watch } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

import NoComponentProvided from './NoComponentProvided.vue';

import type { CustomRenderer } from '../../types';

const NO_DATA_MESSAGE =
    '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.';

const noMatchingComponentMessage = (contentType: string) =>
    `[DotCMSBlockEditorRenderer]: No matching component found for content type: ${contentType}. Provide a custom renderer for this content type to fix this error.`;

/** Renders an embedded contentlet block via a custom renderer keyed by content type. */
const props = defineProps<{
    node: BlockEditorNode;
    customRenderers?: CustomRenderer;
    isDevMode?: boolean;
}>();

const data = computed(() => props.node.attrs?.data as { contentType?: string } | undefined);
const contentType = computed(() => data.value?.contentType ?? 'Unknown Content Type');
const component = computed(() => props.customRenderers?.[contentType.value]);

const state = computed<'no-data' | 'dev-warning' | 'no-component' | 'render'>(() => {
    if (!data.value) {
        return 'no-data';
    }

    if (props.isDevMode && !component.value) {
        return 'dev-warning';
    }

    if (!component.value) {
        return 'no-component';
    }

    return 'render';
});

// Diagnostics as a side effect, kept out of the computed so it stays pure and
// doesn't log twice under SSR.
watch(
    state,
    (value) => {
        if (value === 'no-data') {
            console.error(NO_DATA_MESSAGE);
        } else if (value === 'no-component') {
            console.warn(noMatchingComponentMessage(contentType.value));
        }
    },
    { immediate: true }
);
</script>

<template>
  <NoComponentProvided
    v-if="state === 'dev-warning'"
    :content-type="contentType"
  />
  <component
    :is="component"
    v-else-if="state === 'render'"
    :node="node"
  />
</template>
