<script setup lang="ts">
import { useEditableDotCMSPage } from '@dotcms/vue';
import { computed, ref, watch } from 'vue';

import BlogCard from '@/components/BlogCard.vue';
import Header from '@/components/header/Header.vue';
import { dotCMSClient } from '@/lib/dotCMSClient';
import type { Blog, PageExtraContent } from '@/types/content';
import type { PageResponse } from '@/utils/pageResponse';

const props = defineProps<{ pageResponse: PageResponse }>();

const page = useEditableDotCMSPage(props.pageResponse as never);
const content = computed(() => (page.value?.content ?? {}) as PageExtraContent);
const navigation = computed(() => content.value.navigation);
const allBlogs = computed(() => content.value.blogs ?? []);

const searchQuery = ref('');
const searchResults = ref<Blog[] | null>(null);
const currentYear = new Date().getFullYear();

let debounceTimer: ReturnType<typeof setTimeout> | undefined;

watch(searchQuery, (value) => {
    clearTimeout(debounceTimer);

    if (!value.length) {
        searchResults.value = null;

        return;
    }

    debounceTimer = setTimeout(() => {
        dotCMSClient.content
            .getCollection<Blog>('Blog')
            .limit(3)
            .query((qb) => qb.field('title').equals(`${value}*`))
            .sortBy([{ field: 'Blog.postingDate', order: 'desc' }])
            .then(({ contentlets }) => {
                searchResults.value = contentlets;
            });
    }, 500);
});

// When there is no active search, show the blogs that came with the page.
const filteredBlogs = computed(() => searchResults.value ?? allBlogs.value);
</script>

<template>
    <div class="flex flex-col gap-6 bg-slate-50 min-h-dvh">
        <Header :nav-items="navigation?.children" />
        <main class="container mx-auto px-4 py-8">
            <div class="flex flex-col gap-4 mb-8">
                <h1 class="text-4xl font-bold text-center">Travel Blog</h1>
                <p class="text-gray-600 text-center max-w-2xl mx-auto">
                    Get inspired to experience the world. Our writers give you first-hand stories
                    and recommendations to help you plan your next adventure.
                </p>
            </div>

            <div class="mb-8">
                <div class="relative">
                    <div
                        class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                        <svg
                            class="w-4 h-4 text-gray-500"
                            xmlns="http://www.w3.org/2000/svg"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor">
                            <path
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                :stroke-width="2"
                                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                    </div>
                    <input
                        v-model="searchQuery"
                        type="search"
                        class="block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-white focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Search blogs..." />
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <BlogCard v-for="blog in filteredBlogs" :key="blog.identifier" :blog="blog" />
            </div>

            <div v-if="filteredBlogs.length === 0" class="text-center py-8">
                <p class="text-gray-500">No blogs found matching your search criteria.</p>
            </div>
        </main>
        <footer class="bg-slate-50 text-slate-900 py-4">
            <div class="container mx-auto px-4">
                <p class="text-center">&copy; {{ currentYear }} TravelLux. All rights reserved.</p>
            </div>
        </footer>
    </div>
</template>
