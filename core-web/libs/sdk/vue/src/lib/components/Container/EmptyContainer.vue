<script setup lang="ts">
import { EMPTY_CONTAINER_STYLE_REACT } from '@dotcms/uve/internal';

import { useIsDevMode } from '../../composables/useIsDevMode';

/**
 * @internal
 * Dev-only placeholder for an empty container. The container's `data-dot-*`
 * attributes are forwarded via fallthrough attrs so the editor can target it.
 */
defineOptions({ inheritAttrs: false });

const isDevMode = useIsDevMode();
</script>

<template>
  <div
    v-if="isDevMode"
    v-bind="$attrs"
    :style="EMPTY_CONTAINER_STYLE_REACT"
  >
    <span
      data-testid="empty-container-message"
      data-dot-object="empty-content"
    >
      This container is empty.
    </span>
  </div>
</template>
