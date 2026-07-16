<script setup lang="ts">
import type { DotCMSBasicContentlet } from '@dotcms/types';
import { editContentlet } from '@dotcms/uve';
import { toPlain } from '@dotcms/vue';

import { useIsEditMode } from '@/composables/useIsEditMode';

/** Opens the contentlet editor in the UVE. Only visible in edit mode. */
const props = defineProps<{ contentlet: Partial<DotCMSBasicContentlet> }>();

const isEditMode = useIsEditMode();

const onEdit = (event: MouseEvent) => {
    event.stopPropagation();
    // Unwrap Vue reactivity: UVE posts this to the editor and cannot clone a Proxy.
    editContentlet(toPlain(props.contentlet) as DotCMSBasicContentlet);
};
</script>

<template>
    <button
        v-if="isEditMode"
        type="button"
        class="absolute right-2 top-2 z-20 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-bg shadow-md transition-colors hover:bg-primary-deep"
        @click="onEdit">
        Edit
    </button>
</template>
