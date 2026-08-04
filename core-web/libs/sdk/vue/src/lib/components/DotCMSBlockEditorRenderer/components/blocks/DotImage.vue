<script setup lang="ts">
import { computed, type CSSProperties } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

interface DotCMSImageAttrs {
    src: string;
    alt: string;
    textWrap?: 'left' | 'right';
    textAlign?: string;
}

/** Renders a dotCMS image block with optional text-wrap / alignment. */
const props = defineProps<{ node: BlockEditorNode }>();

const attrs = computed(() => (props.node.attrs ?? {}) as unknown as DotCMSImageAttrs);

const wrapperStyle = computed<CSSProperties>(() => {
    const { textWrap, textAlign } = attrs.value;

    if (textWrap === 'left') {
        return { float: 'left', width: '50%', margin: '0 1rem 1rem 0' };
    }

    if (textWrap === 'right') {
        return { float: 'right', width: '50%', margin: '0 0 1rem 1rem' };
    }

    if (textAlign) {
        return { textAlign: textAlign as CSSProperties['textAlign'] };
    }

    return {};
});

const imgStyle = computed<CSSProperties | undefined>(() =>
    attrs.value.textWrap ? { maxWidth: '100%', height: 'auto' } : undefined
);
</script>

<template>
  <figure :style="wrapperStyle">
    <img
      :alt="attrs.alt"
      :src="attrs.src"
      :style="imgStyle"
    >
  </figure>
</template>
