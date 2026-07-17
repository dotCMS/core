<script setup lang="ts">
import { computed } from 'vue';

import type { DotCMSImage } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

const props = defineProps<{
    fileAsset?: DotCMSImage;
    title?: string;
    description?: string;
}>();

const hasCaption = computed(() => Boolean(props.title || props.description));
const imageSrc = computed(() =>
    props.fileAsset?.identifier ? imageLoader(props.fileAsset.identifier, 1200) : ''
);
</script>

<template>
    <figure class="group relative isolate overflow-hidden rounded-2xl bg-surface-2">
        <div class="relative h-96 w-full">
            <img
                v-if="imageSrc"
                :src="imageSrc"
                class="absolute inset-0 h-full w-full object-cover transition-transform duration-700 ease-(--ease-out-quart) group-hover:scale-105"
                :alt="title ?? ''" />
        </div>
        <figcaption
            v-if="hasCaption"
            class="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/75 via-black/30 to-transparent px-6 pb-6 pt-16 text-bg">
            <p v-if="title" class="font-display text-2xl font-semibold leading-tight">
                {{ title }}
            </p>
            <p v-if="description" class="mt-1 text-sm text-bg/85">{{ description }}</p>
        </figcaption>
    </figure>
</template>
