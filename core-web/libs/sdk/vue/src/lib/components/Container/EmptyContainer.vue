<script setup lang="ts">
import { computed } from 'vue';

import { EMPTY_CONTAINER_STYLE_REACT } from '@dotcms/uve/internal';

import { useDotCMSPageContext } from '../../contexts/dotcms-page.context';

/**
 * @internal
 * Dev-only placeholder for an empty container. The container's `data-dot-*`
 * attributes are forwarded via fallthrough attrs so the editor can target it.
 */
defineOptions({ inheritAttrs: false });

const ctx = useDotCMSPageContext();
const isDevMode = computed(() => ctx.value.isDevMode);
</script>

<template>
    <div v-if="isDevMode" v-bind="$attrs" :style="EMPTY_CONTAINER_STYLE_REACT">
        <span data-testid="empty-container-message" data-dot-object="empty-content">
            This container is empty.
        </span>
    </div>
</template>
