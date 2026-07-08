<script setup lang="ts">
import { computed, type Component } from 'vue';

import type { DotCMSBasicContentlet } from '@dotcms/types';

import { useDotCMSPageContext } from '../../contexts/dotcms-page.context';

/**
 * @internal
 * Fallback rendered when no component is mapped for a contentlet's content type.
 * Only visible in development mode.
 */
const props = defineProps<{
    contentlet: DotCMSBasicContentlet;
    userNoComponent?: Component;
}>();

const ctx = useDotCMSPageContext();
const isDevMode = computed(() => ctx.value.isDevMode);
const useUserComponent = computed(() => !!props.userNoComponent);
</script>

<template>
  <template v-if="isDevMode">
    <component
      :is="userNoComponent"
      v-if="useUserComponent"
      v-bind="contentlet"
    />
    <div
      v-else
      data-testid="no-component"
    >
      No Component for <strong>{{ contentlet.contentType }}</strong>.
    </div>
  </template>
</template>
