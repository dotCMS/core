<script setup lang="ts">
import RecommendedCard from '@/components/RecommendedCard.vue';
import type { Blog } from '@/types/content';

const props = defineProps<{ blogs?: Blog[] }>();
</script>

<template>
    <div v-if="props.blogs?.length" class="flex flex-col">
        <h2 class="mb-6 text-sm font-semibold uppercase tracking-wider text-bg/60">
            Latest stories
        </h2>
        <div class="flex flex-col gap-4">
            <RecommendedCard
                v-for="blog in props.blogs"
                :key="blog.identifier"
                :contentlet="blog" />
        </div>
    </div>
</template>
