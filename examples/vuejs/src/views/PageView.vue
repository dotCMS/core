<script setup lang="ts">
import { shallowRef, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageRenderer from '@/components/PageRenderer.vue';
import { getDotCMSPage } from '@/utils/getDotCMSPage';
import { isPageError, type DotCMSPageContent } from '@/utils/pageResponse';

const route = useRoute();
const router = useRouter();

// shallowRef: keep the page response a plain object. A deep `ref` would wrap it
// (and everything nested) in reactive Proxies, which the UVE bridge cannot
// structured-clone when it posts the page to the editor.
const pageResponse = shallowRef<DotCMSPageContent | null>(null);
const notFound = ref(false);
const loading = ref(true);

const buildPath = () => {
    const match = route.params.pathMatch;
    const segments = Array.isArray(match) ? match : match ? [match] : [];

    return segments.length ? `/${segments.join('/')}` : '/';
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
