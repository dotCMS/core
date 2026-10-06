<script setup lang="ts">
import DestinationListing from '@/components/DestinationListing.vue';
import { useIsEditMode } from '@/composables/useIsEditMode';
import type { Destination } from '@/types/content';

/**
 * Renders VTL-scripted widgets by `componentType`. See:
 * https://dev.dotcms.com/docs/scripting-api#ResponseJSON
 */
const props = defineProps<{
    componentType?: string;
    widgetCodeJSON?: { destinations?: Destination[] };
}>();

const isEditMode = useIsEditMode();
</script>

<template>
    <DestinationListing
        v-if="componentType === 'destinationListing'"
        :destinations="widgetCodeJSON?.destinations" />
    <div v-else-if="isEditMode" class="bg-blue-100 p-4">
        <h4>No Component Type: {{ componentType || 'generic' }} Found for VTL Include</h4>
    </div>
</template>
