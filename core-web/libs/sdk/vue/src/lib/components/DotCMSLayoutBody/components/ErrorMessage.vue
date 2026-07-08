<script setup lang="ts">
import { onMounted } from 'vue';

import { useIsDevMode } from '../../../composables/useIsDevMode';

/** Dev-only message shown when the page is missing its `layout.body`. */
const isDevMode = useIsDevMode();

onMounted(() => {
    console.warn('Missing required layout.body property in page');
});
</script>

<template>
  <div
    v-if="isDevMode"
    data-testid="error-message"
    :style="{ padding: '1rem', border: '1px solid #e0e0e0', borderRadius: '4px' }"
  >
    <p :style="{ margin: '0 0 0.5rem', color: '#666' }">
      The <code>page</code> is missing the required <code>layout.body</code> property.
    </p>
    <p :style="{ margin: 0, color: '#666' }">
      Make sure the page asset is properly loaded and includes a layout configuration.
    </p>
  </div>
</template>
