<script setup lang="ts">
defineProps<{ title?: string; body?: string }>();
</script>

<template>
    <section class="mx-auto w-full max-w-2xl">
        <h2 v-if="title" class="mb-4 font-display text-h2 font-semibold text-ink">{{ title }}</h2>
        <!-- eslint-disable-next-line vue/no-v-html -- renders CMS-authored rich text -->
        <div
            v-if="body"
            class="prose prose-lg max-w-none text-ink prose-headings:font-display prose-a:text-primary hover:prose-a:text-primary-deep"
            v-html="body" />
    </section>
</template>
