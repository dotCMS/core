<script setup lang="ts">
import { computed } from 'vue';

interface YouTubeContent {
    id?: string;
    title?: string;
    author?: string;
    length?: string;
    thumbnailLarge?: string;
}

const props = defineProps<YouTubeContent & { content?: YouTubeContent }>();

const content = computed<YouTubeContent>(() => props.content || props);
const videoId = computed(() => content.value.id);
</script>

<template>
    <figure
        v-if="videoId"
        class="mx-auto w-full max-w-4xl overflow-hidden rounded-2xl border border-line bg-bg shadow-sm">
        <div class="aspect-video w-full bg-surface-2">
            <iframe
                :src="`https://www.youtube.com/embed/${videoId}`"
                :title="content.title"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen
                class="h-full w-full" />
        </div>
        <figcaption class="flex flex-col gap-1 p-5">
            <h3 class="font-display text-xl font-semibold text-ink">{{ content.title }}</h3>
            <div class="flex items-center gap-2 text-sm text-muted">
                <span v-if="content.author">{{ content.author }}</span>
                <span v-if="content.author && content.length" aria-hidden="true">•</span>
                <span v-if="content.length">{{ content.length }}</span>
            </div>
        </figcaption>
    </figure>
</template>
