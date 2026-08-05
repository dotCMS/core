<script setup lang="ts">
/** Fallback (`CustomNoComponent`) rendered when no component maps to a content type. */
defineProps<{ contentType?: string }>();
</script>

<template>
    <div
        class="relative w-full bg-gray-200 h-12 flex justify-center items-center overflow-hidden">
        Don't have a component for this&nbsp;<strong>{{ contentType }}</strong>.
    </div>
</template>
