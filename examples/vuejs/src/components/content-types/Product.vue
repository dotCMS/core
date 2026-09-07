<script setup lang="ts">
import { computed } from 'vue';

import type { DotCMSImage } from '@/types/content';
import { imageLoader } from '@/utils/imageLoader';

// dotCMS returns numeric fields as strings (e.g. "510.00"), so accept both.
const props = defineProps<{
    image?: DotCMSImage;
    title?: string;
    salePrice?: number | string;
    retailPrice?: number | string;
    urlTitle?: string;
}>();

const formatPrice = (price?: number | string) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(
        Number(price ?? 0)
    );

const onSale = computed(() => Boolean(props.retailPrice && props.salePrice));
const displayPrice = computed(() =>
    formatPrice(onSale.value ? props.salePrice : (props.retailPrice ?? props.salePrice))
);
const imageSrc = computed(() =>
    props.image?.idPath ? imageLoader(props.image.idPath, 480) : ''
);
</script>

<template>
    <article
        class="group flex flex-col overflow-hidden rounded-2xl border border-line bg-bg shadow-sm transition-shadow duration-300 hover:shadow-xl hover:shadow-primary-deep/5">
        <div class="relative flex h-56 items-center justify-center border-b border-line bg-bg p-6">
            <img
                v-if="imageSrc"
                class="max-h-full w-auto object-contain transition-transform duration-500 ease-(--ease-out-quart) group-hover:scale-105"
                :src="imageSrc"
                :alt="title || 'Product'" />
            <span
                v-if="onSale"
                class="absolute left-4 top-4 rounded-full bg-accent px-3 py-1 text-xs font-semibold uppercase tracking-wide text-bg">
                Sale
            </span>
        </div>
        <div class="flex flex-1 flex-col p-5">
            <h3 class="line-clamp-1 font-display text-lg font-semibold text-ink">{{ title }}</h3>
            <div class="mt-2 flex items-baseline gap-2">
                <span class="text-2xl font-semibold text-ink">{{ displayPrice }}</span>
                <span v-if="onSale" class="text-sm text-muted line-through">
                    {{ formatPrice(retailPrice) }}
                </span>
            </div>
            <a
                :href="`/store/products/${urlTitle || '#'}`"
                class="group/btn mt-5 inline-flex w-fit items-center gap-1.5 text-sm font-semibold text-primary transition-colors hover:text-primary-deep">
                Buy now
                <span
                    aria-hidden="true"
                    class="transition-transform duration-300 ease-(--ease-out-quart) group-hover/btn:translate-x-0.5">
                    →
                </span>
            </a>
        </div>
    </article>
</template>
