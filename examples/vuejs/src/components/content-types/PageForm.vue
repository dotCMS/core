<script setup lang="ts">
import ContactUs from '@/components/forms/ContactUs.vue';
import { useIsEditMode } from '@/composables/useIsEditMode';
import type { ContentTypeProps } from '@/types/content';

const props = defineProps<ContentTypeProps & { formType?: string; description?: string }>();

const isEditMode = useIsEditMode();
</script>

<template>
    <ContactUs v-if="formType === 'contact-us'" :description="description" />
    <div v-else-if="isEditMode">
        <h4>There is no form component for this form type: {{ formType }}</h4>
    </div>
</template>
