<script setup lang="ts">
import { computed } from 'vue';

import EditButton from '@/components/editor/EditButton.vue';
import type { Blog, Destination } from '@/types/content';
import { formatDate } from '@/utils/formatDate';
import { imageLoader } from '@/utils/imageLoader';

const props = defineProps<{ contentlet: Blog | Destination }>();

const href = computed(() => {
    const url = 'url' in props.contentlet ? props.contentlet.url : undefined;

    return props.contentlet.urlMap || url || '#';
});

const imageSrc = computed(() =>
    props.contentlet.inode ? imageLoader(props.contentlet.inode, 128) : ''
);
</script>

<template>
    <article class="group relative flex items-center gap-4">
        <EditButton :contentlet="(contentlet as never)" />
        <a
            :href="href"
            class="relative aspect-square w-16 shrink-0 overflow-hidden rounded-lg bg-bg/10"
            :tabindex="-1"
            aria-hidden="true">
            <img
                v-if="imageSrc"
                :src="imageSrc"
                alt=""
                class="absolute inset-0 h-full w-full object-cover transition-transform duration-500 ease-(--ease-out-quart) group-hover:scale-105" />
        </a>
        <div class="flex min-w-0 flex-col gap-1">
            <a
                :href="href"
                class="line-clamp-2 text-sm font-medium leading-snug text-bg transition-colors hover:text-accent-soft">
                {{ contentlet.title }}
            </a>
            <time v-if="contentlet.modDate" class="text-xs text-bg/55">
                {{ formatDate(contentlet.modDate) }}
            </time>
        </div>
    </article>
</template>
