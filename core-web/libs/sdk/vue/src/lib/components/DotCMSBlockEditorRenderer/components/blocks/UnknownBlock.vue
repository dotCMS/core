<script setup lang="ts">
import { onMounted } from 'vue';

import type { BlockEditorNode } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

/**
 * Renders an unknown block. Shows a visible warning only inside the UVE; in the
 * live site it renders nothing to avoid leaking editor scaffolding.
 */
const props = defineProps<{ node: BlockEditorNode }>();

const boxStyle = {
    backgroundColor: '#fff5f5',
    color: '#333',
    padding: '1rem',
    borderRadius: '0.5rem',
    marginBottom: '1rem',
    marginTop: '1rem',
    border: '1px solid #fc8181'
};

onMounted(() => {
    if (!getUVEState()) {
        console.warn(
            `[DotCMSBlockEditorRenderer]: Unknown block type "${props.node?.type}". No renderer was found for it.`
        );
    }
});
</script>

<template>
  <div
    v-if="getUVEState()"
    data-testid="unknown-block"
    :style="boxStyle"
  >
    Unknown block type:
    <strong>{{ node?.type }}</strong>
  </div>
</template>
