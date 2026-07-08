<script setup lang="ts">
import { computed } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';

interface DotCMSVideoAttrs {
    data?: { identifier: string; thumbnail: string };
    src: string;
    mimeType: string;
    width: number;
    height: number;
}

/** Renders a dotCMS video block. */
const props = defineProps<{ node: BlockEditorNode }>();

const attrs = computed(() => (props.node.attrs ?? {}) as unknown as DotCMSVideoAttrs);
const poster = computed(() => attrs.value.data?.thumbnail);
</script>

<template>
  <video
    controls
    preload="metadata"
    :width="attrs.width"
    :height="attrs.height"
    :poster="poster"
  >
    <track
      default
      kind="captions"
      srclang="en"
    >
    <source
      :src="attrs.src"
      :type="attrs.mimeType"
    >
    Your browser does not support the <code>video</code> element.
  </video>
</template>
