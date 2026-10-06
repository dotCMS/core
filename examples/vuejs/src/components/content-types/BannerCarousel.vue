<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import { imageLoader } from '@/utils/imageLoader';

interface CarouselBanner {
    image: string;
    title: string;
}

const props = defineProps<{ widgetCodeJSON?: { banners?: CarouselBanner[] } }>();

const banners = computed(() => props.widgetCodeJSON?.banners ?? []);
const currentIndex = ref(0);
let timer: ReturnType<typeof setInterval> | undefined;

const nextSlide = () => {
    const length = banners.value.length || 1;
    currentIndex.value = (currentIndex.value + 1) % length;
};

const prevSlide = () => {
    const length = banners.value.length || 1;
    currentIndex.value = (currentIndex.value - 1 + length) % length;
};

onMounted(() => {
    timer = setInterval(nextSlide, 3000);
});

onBeforeUnmount(() => {
    if (timer) {
        clearInterval(timer);
    }
});
</script>

<template>
    <div v-if="banners.length" class="relative w-full mx-auto">
        <div class="overflow-hidden relative h-96">
            <div
                v-for="(banner, index) in banners"
                :key="index"
                class="duration-700 ease-in-out w-full h-full"
                :class="index === currentIndex ? '' : 'hidden'">
                <img
                    :src="imageLoader(banner.image, 1600)"
                    class="absolute block w-full h-full object-cover"
                    :alt="banner.title" />
            </div>
        </div>
        <button
            type="button"
            class="absolute top-0 start-0 z-30 flex items-center justify-center h-full px-4 cursor-pointer group focus:outline-none"
            @click="prevSlide">
            <span
                class="inline-flex items-center justify-center w-10 h-10 rounded-full bg-white/30 group-hover:bg-white/50 group-focus:ring-4 group-focus:ring-white group-focus:outline-none">
                <svg
                    class="w-4 h-4 text-white rtl:rotate-180"
                    aria-hidden="true"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 6 10">
                    <path
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M5 1 1 5l4 4" />
                </svg>
                <span class="sr-only">Previous</span>
            </span>
        </button>
        <button
            type="button"
            class="absolute top-0 end-0 z-30 flex items-center justify-center h-full px-4 cursor-pointer group focus:outline-none"
            @click="nextSlide">
            <span
                class="inline-flex items-center justify-center w-10 h-10 rounded-full bg-white/30 group-hover:bg-white/50 group-focus:ring-4 group-focus:ring-white group-focus:outline-none">
                <svg
                    class="w-4 h-4 text-white rtl:rotate-180"
                    aria-hidden="true"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 6 10">
                    <path
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="m1 9 4-4-4-4" />
                </svg>
                <span class="sr-only">Next</span>
            </span>
        </button>
    </div>
</template>
