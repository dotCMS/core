<script setup lang="ts">
import type { DotCMSBasicContentlet } from '@dotcms/types';
import { computed } from 'vue';

import EditButton from '@/components/editor/EditButton.vue';
import { imageLoader } from '@/utils/imageLoader';

type StoreProduct = DotCMSBasicContentlet & {
    image: string;
    title: string;
    retailPrice?: number | string;
    salePrice?: number | string;
};

const props = defineProps<{
    widgetTitle?: string;
    widgetCodeJSON: { products?: StoreProduct[] };
}>();

const products = computed(() => props.widgetCodeJSON?.products ?? []);

const hasDiscount = (p: StoreProduct) =>
    Boolean(p.salePrice && p.retailPrice && Number(p.salePrice) < Number(p.retailPrice));

const discountPct = (p: StoreProduct) =>
    Math.round((1 - Number(p.salePrice) / Number(p.retailPrice)) * 100);
</script>

<template>
    <div>
        <h2 class="text-4xl font-bold mb-4">{{ widgetTitle }}</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            <div
                v-for="product in products"
                :key="product.identifier"
                class="group relative">
                <EditButton :contentlet="product" />
                <div
                    class="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow duration-300 flex flex-col h-full">
                    <div class="relative aspect-square w-full overflow-hidden bg-gray-100">
                        <img
                            :src="imageLoader(product.image, 600)"
                            :alt="product.title"
                            class="absolute inset-0 h-full w-full object-cover object-center group-hover:scale-105 transition-transform duration-300" />
                    </div>
                    <div class="p-4 flex flex-col grow">
                        <h3
                            class="text-gray-900 font-medium text-sm mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors">
                            {{ product.title }}
                        </h3>
                        <div class="mt-auto pt-2 flex items-center justify-between">
                            <div v-if="hasDiscount(product)">
                                <span class="text-red-600 font-semibold mr-2">
                                    ${{ product.salePrice }}
                                </span>
                                <span class="text-gray-500 text-sm line-through">
                                    ${{ product.retailPrice }}
                                </span>
                            </div>
                            <span v-else class="text-gray-900 font-semibold">
                                ${{ product.retailPrice }}
                            </span>
                            <span
                                v-if="hasDiscount(product)"
                                class="bg-red-100 text-red-800 text-xs px-2 py-1 rounded-full">
                                {{ discountPct(product) }}% OFF
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
