<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageRenderer from '@/components/PageRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import { isPageError, type PageResponse } from '@/utils/pageResponse';

const route = useRoute();
const router = useRouter();

const pageResponse = ref<PageResponse | null>(null);
const notFound = ref(false);
const loading = ref(true);

const buildPath = () => {
    const match = route.params.pathMatch;
    const segments = Array.isArray(match) ? match : match ? [match] : [];

    return segments.length ? `/${segments.join('/')}` : '/';
};

const loadPage = async () => {
    loading.value = true;
    notFound.value = false;
    pageResponse.value = null;

    const response = await getDotCMSPage(buildPath());

    if (isPageError(response)) {
        notFound.value = true;
        loading.value = false;

        return;
    }

    const vanityUrl = response.pageAsset?.vanityUrl;
    if (vanityUrl?.action && vanityUrl.action > 200 && vanityUrl.forwardTo) {
        router.replace(vanityUrl.forwardTo);

        return;
    }

    if (!response.pageAsset) {
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
    <!-- Remount PageRenderer per path so the editable-page composable re-inits. -->
    <PageRenderer v-if="pageResponse" :key="route.fullPath" :page-response="pageResponse" />

    <div
        v-else-if="notFound"
        class="flex min-h-dvh items-center justify-center bg-bg p-8">
        <div class="text-center">
            <h1 class="text-h2 font-display">Page not found</h1>
            <p class="mt-2 text-muted">We couldn't find the page you were looking for.</p>
            <RouterLink to="/" class="mt-6 inline-block text-primary underline">
                Back home
            </RouterLink>
        </div>
    </div>

    <div v-else-if="loading" class="flex min-h-dvh items-center justify-center bg-bg">
        <p class="text-muted">Loading…</p>
    </div>
</template>
