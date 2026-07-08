<script setup lang="ts">
import { onMounted } from 'vue';

import { EMPTY_CONTAINER_STYLE_REACT } from '@dotcms/uve/internal';

import { useIsDevMode } from '../../composables/useIsDevMode';

/** @internal Dev-only message shown when a container cannot be found on the page. */
const props = defineProps<{ identifier: string }>();

const isDevMode = useIsDevMode();

onMounted(() => {
    if (isDevMode.value) {
        console.error(`Container with identifier ${props.identifier} not found`);
    }
});
</script>

<template>
  <div
    v-if="isDevMode"
    data-testid="container-not-found"
    :style="EMPTY_CONTAINER_STYLE_REACT"
  >
    This container with identifier {{ identifier }} was not found.
  </div>
</template>
