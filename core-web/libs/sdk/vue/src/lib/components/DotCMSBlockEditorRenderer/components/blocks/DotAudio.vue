<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

interface DotCMSAudioAttrs {
    src: string;
    mimeType: string;
}

/** Renders a dotCMS audio block. Mirrors DotVideo without poster/width/height. */
const props = defineProps<{ node: BlockEditorNode }>();

const attrs = computed(() => (props.node.attrs ?? {}) as unknown as DotCMSAudioAttrs);
</script>

<template>
    <audio controls preload="metadata">
        <source :src="attrs.src" :type="attrs.mimeType" />
        Your browser does not support the
        <code>audio</code>
        element.
    </audio>
</template>
