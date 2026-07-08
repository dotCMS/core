<script setup lang="ts">
import { computed, markRaw, toRaw, type Component } from 'vue';

import { isValidBlocks } from '@dotcms/uve/internal';

import BlockEditorBlock from './components/BlockEditorBlock.vue';
import type { BlockEditorRendererProps, CustomRenderer } from './types';

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

// Unwrap + mark the custom renderers raw once at the entry point so the recursive
// dispatcher never renders a reactive-proxied component via `<component :is>`
// (which triggers Vue's "Component that was made reactive" warning).
const rawCustomRenderers = computed<CustomRenderer | undefined>(() => {
    if (!props.customRenderers) {
        return undefined;
    }

    const out: CustomRenderer = {};
    for (const key of Object.keys(props.customRenderers)) {
        out[key] = toRaw(props.customRenderers[key]) as Component;
    }

    return markRaw(out);
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
            :custom-renderers="rawCustomRenderers"
            :is-dev-mode="isDevMode" />
    </div>
</template>
