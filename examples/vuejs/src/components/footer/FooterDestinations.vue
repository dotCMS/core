<script setup lang="ts">
import RecommendedCard from '@/components/RecommendedCard.vue';
import type { Destination } from '@/types/content';

const props = defineProps<{ destinations?: Destination[] }>();
</script>

<template>
    <div v-if="props.destinations?.length" class="flex flex-col">
        <h2 class="mb-6 text-sm font-semibold uppercase tracking-wider text-bg/60">
            Popular destinations
        </h2>
        <div class="flex flex-col gap-4">
            <RecommendedCard
                v-for="destination in props.destinations"
                :key="destination.identifier"
                :contentlet="destination" />
        </div>
    </div>
</template>
