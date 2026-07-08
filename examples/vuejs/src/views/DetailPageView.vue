<script setup lang="ts">
import { shallowRef, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import DetailRenderer from '@/components/DetailRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import { isPageError, type DotCMSPageContent } from '@/utils/pageResponse';

const route = useRoute();

// shallowRef: keep the page response plain so the UVE bridge can clone it.
const pageResponse = shallowRef<DotCMSPageContent | null>(null);
const notFound = ref(false);
const loading = ref(true);

const buildPath = () => {
    const slug = route.params.slug;
    const value = Array.isArray(slug) ? slug[0] : slug;

    return `/blog/post/${value ?? ''}`;
};

// `onCleanup` runs when the route changes again before the fetch resolves, so a
// slower earlier request can't overwrite state for the newer route.
const loadPage = async (_path: string, _prev: string | undefined, onCleanup: (fn: () => void) => void) => {
    let cancelled = false;
    onCleanup(() => {
        cancelled = true;
    });

    loading.value = true;
    notFound.value = false;
    pageResponse.value = null;

    const response = await getDotCMSPage(buildPath());
    if (cancelled) {
        return;
    }

    if (isPageError(response) || !response.pageAsset) {
        notFound.value = true;
        loading.value = false;

        return;
    }

    pageResponse.value = response;
    loading.value = false;
};

watch(() => route.fullPath, loadPage, { immediate: true });
</script>

<template>
    <DetailRenderer v-if="pageResponse" :key="route.fullPath" :page-response="pageResponse" />

    <div
        v-else-if="notFound"
        class="flex min-h-dvh items-center justify-center bg-slate-50 p-8">
        <div class="text-center">
            <h1 class="text-h2 font-display">Post not found</h1>
            <RouterLink to="/blog" class="mt-6 inline-block text-primary underline">
                Back to the blog
            </RouterLink>
        </div>
    </div>

    <div v-else-if="loading" class="flex min-h-dvh items-center justify-center bg-slate-50">
        <p class="text-muted">Loading…</p>
    </div>
</template>
