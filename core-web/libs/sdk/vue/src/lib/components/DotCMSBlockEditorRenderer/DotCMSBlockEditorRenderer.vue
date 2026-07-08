<script setup lang="ts">
import { computed } from 'vue';

import { isValidBlocks } from '@dotcms/uve/internal';

import BlockEditorBlock from './components/BlockEditorBlock.vue';
import type { BlockEditorRendererProps } from './types';

/**
 * Renders a dotCMS Block Editor field.
 *
 * @example
 * ```vue
 * <DotCMSBlockEditorRenderer :blocks="contentlet.body" :custom-renderers="renderers" />
 * ```
 */
const props = withDefaults(defineProps<BlockEditorRendererProps>(), {
    isDevMode: false
});

const validation = computed(() => isValidBlocks(props.blocks));
const errorMessage = computed(() => {
    const error = validation.value.error;

    if (error) {
        console.error(error);
    }

    return error;
});
</script>

<template>
    <div v-if="errorMessage && isDevMode" data-testid="invalid-blocks-message">
        {{ errorMessage }}
    </div>
    <div
        v-else-if="!errorMessage"
        :class="className"
        :style="style"
        data-testid="dot-block-editor-container">
        <BlockEditorBlock
            :content="blocks?.content"
            :custom-renderers="customRenderers"
            :is-dev-mode="isDevMode" />
    </div>
</template>
