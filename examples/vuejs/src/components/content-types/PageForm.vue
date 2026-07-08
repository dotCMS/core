<script setup lang="ts">
import ContactUs from '@/components/forms/ContactUs.vue';
import { useIsEditMode } from '@/composables/useIsEditMode';

// Declare only the fields this component reads (the whole contentlet is spread
// onto it, and Vue runtime-validates every declared prop).
defineProps<{ formType?: string; description?: string }>();

const isEditMode = useIsEditMode();
</script>

<template>
    <ContactUs v-if="formType === 'contact-us'" :description="description" />
    <div v-else-if="isEditMode">
        <h4>There is no form component for this form type: {{ formType }}</h4>
    </div>
</template>
