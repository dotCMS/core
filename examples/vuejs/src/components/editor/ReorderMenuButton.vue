<script setup lang="ts">
import { reorderMenu } from '@dotcms/uve';

import { useIsEditMode } from '@/composables/useIsEditMode';

/** Triggers the UVE menu-reorder flow. Only visible in edit mode. */
const isEditMode = useIsEditMode();

const onReorder = () => reorderMenu({ startLevel: 1, depth: 2 });
</script>

<template>
    <button
        v-if="isEditMode"
        type="button"
        class="rounded-full border border-line px-3 py-1.5 text-sm font-medium text-ink transition-colors hover:border-primary/40 hover:text-primary"
        @click="onReorder">
        Reorder menu
    </button>
</template>
