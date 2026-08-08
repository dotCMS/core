<script setup lang="ts">
import { computed } from 'vue';

interface Category {
    title: string;
    url: string;
}

const props = defineProps<{ widgetCodeJSON: { categories?: Category[] } }>();

const categories = computed(() => props.widgetCodeJSON?.categories ?? []);
</script>

<template>
    <div v-if="categories.length" class="rounded-2xl border border-line bg-bg p-6">
        <h3 class="mb-4 font-display text-lg font-semibold text-ink">Categories</h3>
        <ul class="flex flex-wrap gap-2">
            <li v-for="category in categories" :key="category.url">
                <a
                    :href="category.url"
                    class="inline-flex rounded-full border border-line px-3.5 py-1.5 text-sm text-ink transition-colors hover:border-primary/40 hover:bg-primary-tint hover:text-primary">
                    {{ category.title }}
                </a>
            </li>
        </ul>
    </div>
</template>
